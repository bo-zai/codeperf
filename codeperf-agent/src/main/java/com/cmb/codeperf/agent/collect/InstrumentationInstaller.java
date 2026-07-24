package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.collect.advice.EntryAdvice;
import com.cmb.codeperf.agent.collect.advice.JdbcPrepareBindAdvice;
import com.cmb.codeperf.agent.collect.advice.JdbcPreparedExecAdvice;
import com.cmb.codeperf.agent.collect.advice.JdbcStatementAdvice;
import com.cmb.codeperf.agent.collect.advice.MethodTraceAdvice;
import com.cmb.codeperf.agent.config.AgentConfig;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * 用 ByteBuddy 把各 Advice 织入目标类。正式环境通过 premain 在类加载时织入。
 * 见 docs/02-agent-core.md 第 2、5 节。
 *
 * 关键点：Advice 被内联到目标方法，目标类的 ClassLoader 只需能解析 {@link Recorder}
 * （位于 system ClassLoader，是 app/驱动 ClassLoader 的祖先），故无需把 Advice 类暴露给目标。
 */
public class InstrumentationInstaller {

    public void install(AgentConfig cfg, Instrumentation inst) {
        ElementMatcher.Junction<TypeDescription> ignoredPackages = buildPackageMatcher(cfg.getExcludedPackages());
        ElementMatcher.Junction<TypeDescription> targetPkg =
                buildApplicationMatcher(cfg.getTargetPackages(), cfg.getExcludedPackages());

        AgentBuilder builder = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(buildIgnoreMatcher(ignoredPackages))
                .with(new CodePerfAgentListener());

        // 1) 应用包方法级插桩
        if (targetPkg != null) {
            builder = builder.type(targetPkg).transform((b, td, cl, module, pd) ->
                    b.visit(Advice.to(MethodTraceAdvice.class).on(
                            not(isConstructor())
                                    .and(not(isAbstract()))
                                    .and(not(isNative()))
                                    .and(not(isSynthetic())))));
        }

        // 2) 入口识别：DispatcherServlet#doDispatch
        builder = builder.type(named("org.springframework.web.servlet.DispatcherServlet"))
                .transform((b, td, cl, module, pd) ->
                        b.visit(Advice.to(EntryAdvice.class).on(named("doDispatch"))));

        // 3) JDBC：Statement.execute*(String ...)
        builder = builder.type(not(isInterface()).and(hasSuperType(named("java.sql.Statement"))))
                .transform((b, td, cl, module, pd) ->
                        b.visit(Advice.to(JdbcStatementAdvice.class).on(
                                namedOneOf("execute", "executeQuery", "executeUpdate")
                                        .and(takesArgument(0, String.class)))));

        // 4) JDBC：PreparedStatement 无参 execute*()
        builder = builder.type(not(isInterface()).and(hasSuperType(named("java.sql.PreparedStatement"))))
                .transform((b, td, cl, module, pd) ->
                        b.visit(Advice.to(JdbcPreparedExecAdvice.class).on(
                                namedOneOf("execute", "executeQuery", "executeUpdate")
                                        .and(takesArguments(0)))));

        // 5) JDBC：Connection#prepareStatement(String ...) 绑定 SQL
        builder = builder.type(not(isInterface()).and(hasSuperType(named("java.sql.Connection"))))
                .transform((b, td, cl, module, pd) ->
                        b.visit(Advice.to(JdbcPrepareBindAdvice.class).on(
                                named("prepareStatement").and(takesArgument(0, String.class)))));

        builder.installOn(inst);
    }

    ElementMatcher.Junction<TypeDescription> buildPackageMatcher(List<String> packages) {
        ElementMatcher.Junction<TypeDescription> matcher = null;
        for (String pkg : packages) {
            String p = normalizePackagePrefix(pkg);
            if (p.isEmpty()) {
                continue;
            }
            // ByteBuddy 的 nameStartsWith 是纯字符串匹配，必须补包名边界，避免 com.cmb 误匹配 com.cmbchina。
            ElementMatcher.Junction<TypeDescription> one = nameStartsWith(p + ".");
            matcher = (matcher == null) ? one : matcher.or(one);
        }
        return matcher;
    }

    ElementMatcher.Junction<TypeDescription> buildApplicationMatcher(List<String> targetPackages,
                                                                     List<String> excludedPackages) {
        ElementMatcher.Junction<TypeDescription> targetMatcher = buildPackageMatcher(targetPackages);
        ElementMatcher.Junction<TypeDescription> excludedMatcher = buildPackageMatcher(excludedPackages);
        if (targetMatcher == null || excludedMatcher == null) {
            return targetMatcher;
        }
        // 排除包只作用于业务方法追踪，避免误关闭 DispatcherServlet/JDBC 等核心 I/O 证据采集。
        return targetMatcher.and(not(excludedMatcher));
    }

    ElementMatcher.Junction<TypeDescription> buildIgnoreMatcher(ElementMatcher.Junction<TypeDescription> ignoredPackages) {
        ElementMatcher.Junction<TypeDescription> matcher = nameStartsWith("net.bytebuddy.")
                .or(nameStartsWith("com.cmb.codeperf.agent."))
                .or(isSynthetic());
        if (ignoredPackages == null) {
            return matcher;
        }
        // 公司基础框架类优先全局忽略，避免 matcher 解析其 optional/provided 依赖时影响应用启动。
        return matcher.or(ignoredPackages);
    }

    private String normalizePackagePrefix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

