package com.cmb.codeperf.analysis.staticanalysis.rules;

import com.cmb.codeperf.analysis.Severity;
import com.cmb.codeperf.analysis.staticanalysis.BytecodeRule;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis.CallSite;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis.MethodAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding.Confidence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 规则 3.1：循环内调用远程操作 → 疑似 N+1。
 *
 * <p>分支 A：调 ORM/Repository/DAO/Mapper（数据库 N+1，HIGH）。
 * <p>分支 B：调远程 API（网络 N+1）：Feign / RestTemplate / WebClient / 通用 HttpClient / 跨包 Service。
 *
 * <p>一个方法内若多个循环调用命中多条信号，仅记录置信度最高的一条。
 * 见 docs/05-static-analysis.md 第 3.1 节。
 */
public class NPlusOneSuspect implements BytecodeRule {

    private static final String TYPE = "N+1 嫌疑（循环内远程操作）";

    private static final Set<String> REST_TEMPLATE_METHODS = new HashSet<>(Arrays.asList(
            "getForObject", "getForEntity", "postForObject", "postForEntity", "exchange"));
    private static final Set<String> WEBCLIENT_METHODS = new HashSet<>(Arrays.asList(
            "get", "put", "post", "delete", "patch"));

    @Override
    public List<StaticFinding> analyze(List<ClassAnalysis> classes, String targetPackage) {
        String pkgInternal = targetPackage == null ? "" : targetPackage.replace('.', '/');
        Set<String> feignClasses = collectFeignClasses(classes);

        List<StaticFinding> findings = new ArrayList<>();
        for (ClassAnalysis cls : classes) {
            for (MethodAnalysis m : cls.getMethods()) {
                if (m.getLoopRanges().isEmpty()) {
                    continue;
                }

                Confidence best = null;
                CallSite bestCall = null;
                String bestReason = null;

                for (CallSite call : m.getCalls()) {
                    if (!m.inLoop(call.getInsnIdx())) {
                        continue;
                    }

                    String[] hit = classify(call, pkgInternal, feignClasses);
                    if (hit == null) {
                        continue;
                    }
                    Confidence c = Confidence.valueOf(hit[0]);
                    if (best == null || c.ordinal() > best.ordinal()) {
                        best = c;
                        bestCall = call;
                        bestReason = hit[1];
                    }
                }

                if (best != null) {
                    String classMethod = cls.getClassName() + "." + m.getName();
                    String evidence = bestReason + "：循环内调用 "
                            + bestCall.getOwner().replace('/', '.') + "#" + bestCall.getName()
                            + "()（指令 #" + bestCall.getInsnIdx() + "）";
                    findings.add(new StaticFinding(
                            TYPE, Severity.WARN, best,
                            "方法在循环体内执行远程/数据访问操作，可能是 N+1：1 次批量被拆成 N 次独立 I/O。",
                            evidence, classMethod));
                }
            }
        }
        return findings;
    }

    /** 返回 [confidence, reason] 或 null（未命中）。 */
    private String[] classify(CallSite call, String pkgInternal, Set<String> feignClasses) {
        String owner = call.getOwner();

        // 分支 A：数据访问层
        if (owner.contains("Repository") || owner.contains("DAO")
                || owner.contains("Dao") || owner.contains("Mapper")) {
            return new String[]{"HIGH", "数据访问层（Repository/DAO/Mapper）"};
        }

        // 分支 B：Feign 客户端
        if (call.isInterface()
                && (owner.contains("FeignClient") || owner.endsWith("Client")
                    || feignClasses.contains(owner))) {
            return new String[]{"HIGH", "Feign/远程客户端接口"};
        }

        // 分支 B：RestTemplate
        if ("org/springframework/web/client/RestTemplate".equals(owner)
                && REST_TEMPLATE_METHODS.contains(call.getName())) {
            return new String[]{"HIGH", "RestTemplate HTTP 调用"};
        }

        // 分支 B：WebClient
        if ("org/springframework/web/reactive/function/client/WebClient".equals(owner)
                && WEBCLIENT_METHODS.contains(call.getName())) {
            return new String[]{"HIGH", "WebClient 响应式 HTTP 调用"};
        }

        // 分支 B：通用 HTTP 客户端
        if ((owner.contains("HttpClient") || owner.contains("OkHttpClient")
                || owner.contains("RestClient")) && !"<init>".equals(call.getName())) {
            return new String[]{"MEDIUM", "通用 HTTP 客户端"};
        }

        // 分支 B：跨包 Service（外部模块的 Service，可能发远程调用）
        if (owner.endsWith("Service")
                && !pkgInternal.isEmpty()
                && !owner.startsWith(pkgInternal)
                && !owner.startsWith("java/") && !owner.startsWith("javax/")) {
            return new String[]{"LOW", "跨包 Service 调用（疑似远程）"};
        }

        return null;
    }

    private Set<String> collectFeignClasses(List<ClassAnalysis> classes) {
        Set<String> set = new HashSet<>();
        for (ClassAnalysis cls : classes) {
            if (cls.isFeignClient()) {
                set.add(cls.getClassName().replace('.', '/'));
            }
        }
        return set;
    }
}

