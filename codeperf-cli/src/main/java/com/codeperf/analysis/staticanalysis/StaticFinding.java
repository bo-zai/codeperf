package com.codeperf.analysis.staticanalysis;

import com.codeperf.analysis.Severity;

/**
 * 单条静态发现。比动态 Finding 多了 confidence 和 classMethod。
 * 见 docs/05-static-analysis.md 第 3 节。
 */
public class StaticFinding {

    public enum Confidence { LOW, MEDIUM, HIGH }

    private final String type;
    private final Severity severity;
    private final Confidence confidence;
    private final String description;
    private final String evidence;
    private final String classMethod; // 来源方法，如 com.demo.Service.method

    public StaticFinding(String type, Severity severity, Confidence confidence,
                         String description, String evidence, String classMethod) {
        this.type = type;
        this.severity = severity;
        this.confidence = confidence;
        this.description = description;
        this.evidence = evidence;
        this.classMethod = classMethod;
    }

    public String getType() { return type; }
    public Severity getSeverity() { return severity; }
    public Confidence getConfidence() { return confidence; }
    public String getDescription() { return description; }
    public String getEvidence() { return evidence; }
    public String getClassMethod() { return classMethod; }
}
