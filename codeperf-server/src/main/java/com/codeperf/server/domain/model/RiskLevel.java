package com.codeperf.server.domain.model;

public enum RiskLevel {
    NONE(0),
    INFO(1),
    WARN(2),
    CRITICAL(3);

    private final int level;

    RiskLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static RiskLevel max(RiskLevel left, RiskLevel right) {
        return left.level >= right.level ? left : right;
    }
}
