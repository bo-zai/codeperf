package com.codeperf.agent.collect;

import com.codeperf.agent.collect.advice.EntryAdvice;
import com.codeperf.agent.collect.advice.JdbcPrepareBindAdvice;
import com.codeperf.agent.collect.advice.JdbcPreparedExecAdvice;
import com.codeperf.agent.collect.advice.JdbcStatementAdvice;
import com.codeperf.agent.collect.advice.MethodTraceAdvice;
import com.codeperf.agent.config.AgentConfig;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.PrintStream;
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
        ElementMatcher.Junction<TypeDescription> targetPkg = buildPackageMatcher(cfg.getTargetPackages());

        AgentBuilder builder = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .ignore(nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("com.codeperf.agent."))
                        .or(isSynthetic()))
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut()
                        .withTransformationsOnly());

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

    private ElementMatcher.Junction<TypeDescription> buildPackageMatcher(List<String> packages) {
        ElementMatcher.Junction<TypeDescription> matcher = null;
        for (String pkg : packages) {
            String p = pkg.trim();
            if (p.isEmpty()) {
                continue;
            }
            ElementMatcher.Junction<TypeDescription> one = nameStartsWith(p);
            matcher = (matcher == null) ? one : matcher.or(one);
        }
        return matcher;
    }
}
