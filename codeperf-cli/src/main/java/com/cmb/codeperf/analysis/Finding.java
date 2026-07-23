package com.cmb.codeperf.analysis;

/**
 * 单条检测发现：类型 + 严重度 + 描述 + 证据。
 * 见 docs/04-analysis-report.md 第 3 节。
 */
public class Finding {

    private final String type;
    private final Severity severity;
    private final String description;
    private final String evidence;

    public Finding(String type, Severity severity, String description, String evidence) {
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.evidence = evidence;
    }

    public String getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getEvidence() {
        return evidence;
    }
}

