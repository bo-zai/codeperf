package com.cmb.codeperf.analysis.staticanalysis.rules;

import com.cmb.codeperf.analysis.Severity;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRule;
import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRuleContext;

import java.util.ArrayList;
import java.util.List;

public class LoopIoAmplificationRule implements StaticRule {

    private static final String TYPE = "Loop I/O Amplification";

    @Override
    public String id() {
        return "loop-io-amplification";
    }

    @Override
    public String displayName() {
        return "循环内外部 I/O 放大风险";
    }

    @Override
    public List<StaticFinding> analyze(StaticRuleContext context) {
        String pkgInternal = context.getTargetPackage().replace('.', '/');
        List<StaticFinding> findings = new ArrayList<>();
        for (ClassAnalysis cls : context.getClasses()) {
            for (ClassAnalysis.MethodAnalysis method : cls.getMethods()) {
                if (method.getLoopRanges().isEmpty()) {
                    continue;
                }
                for (ClassAnalysis.CallSite call : method.getCalls()) {
                    if (!method.inLoop(call.getInsnIdx())) {
                        continue;
                    }
                    Classification classification = classify(call, pkgInternal);
                    if (classification == null) {
                        continue;
                    }
                    findings.add(toFinding(cls, method, call, classification));
                }
            }
        }
        return findings;
    }

    private StaticFinding toFinding(ClassAnalysis cls, ClassAnalysis.MethodAnalysis method,
                                    ClassAnalysis.CallSite call, Classification classification) {
        ClassAnalysis.LoopRange range = findLoopRange(method, call.getInsnIdx());
        String className = cls.getClassName();
        String callOwner = call.getOwner().replace('/', '.');
        String classMethod = className + "." + method.getName();
        String evidence = classification.reason + ": loop calls "
                + callOwner + "#" + call.getName() + "() at line " + call.getLineNumber();
        return new StaticFinding(
                TYPE,
                Severity.WARN,
                classification.confidence,
                "方法循环体内存在外部 I/O 调用，可能被生产数据规模线性放大。",
                evidence,
                classMethod,
                cls.getSourceFile(),
                call.getLineNumber(),
                range == null ? 0 : range.getStartLine(),
                range == null ? 0 : range.getEndLine(),
                className,
                method.getName(),
                callOwner,
                call.getName(),
                classification.ioType);
    }

    private ClassAnalysis.LoopRange findLoopRange(ClassAnalysis.MethodAnalysis method, int insnIdx) {
        for (ClassAnalysis.LoopRange range : method.getLoopLineRanges()) {
            if (range.getStartInsn() <= insnIdx && insnIdx <= range.getEndInsn()) {
                return range;
            }
        }
        return null;
    }

    private Classification classify(ClassAnalysis.CallSite call, String pkgInternal) {
        String owner = call.getOwner();
        if (containsAny(owner, "Repository", "DAO", "Dao", "Mapper", "JdbcTemplate")) {
            return new Classification("DB", StaticFinding.Confidence.HIGH, "database access");
        }
        if (owner.contains("FeignClient")
                || owner.endsWith("Client")
                || "org/springframework/web/client/RestTemplate".equals(owner)
                || "org/springframework/web/reactive/function/client/WebClient".equals(owner)
                || containsAny(owner, "OkHttpClient", "HttpClient", "RestClient")) {
            return new Classification("HTTP", StaticFinding.Confidence.HIGH, "HTTP client");
        }
        if (containsAny(owner, "Dubbo", "Rpc", "Grpc", "Thrift")) {
            return new Classification("RPC", StaticFinding.Confidence.HIGH, "RPC client");
        }
        if (containsAny(owner, "Sdk", "SDK", "Gateway", "Facade")) {
            return new Classification("SDK", StaticFinding.Confidence.MEDIUM, "external SDK/gateway");
        }
        if (owner.endsWith("Service")
                && !pkgInternal.isEmpty()
                && !owner.startsWith(pkgInternal)
                && !owner.startsWith("java/")
                && !owner.startsWith("javax/")) {
            return new Classification("SERVICE", StaticFinding.Confidence.LOW, "cross-package service");
        }
        return null;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static class Classification {
        final String ioType;
        final StaticFinding.Confidence confidence;
        final String reason;

        Classification(String ioType, StaticFinding.Confidence confidence, String reason) {
            this.ioType = ioType;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}

