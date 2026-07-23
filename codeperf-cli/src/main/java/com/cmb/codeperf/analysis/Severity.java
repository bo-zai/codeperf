package com.cmb.codeperf.analysis;

/**
 * 问题严重度，数值越大越严重。见 docs/04-analysis-report.md 第 4 节。
 */
public enum Severity {
    INFO(1),
    WARN(2),
    CRITICAL(3);

    private final int level;

    Severity(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}

