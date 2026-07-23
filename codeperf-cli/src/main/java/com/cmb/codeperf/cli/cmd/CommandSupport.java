package com.cmb.codeperf.cli.cmd;

/**
 * CLI 命令通用辅助方法。
 * 只放命令层共享的小逻辑，避免每个命令重复参数校验和风险等级判断。
 */
final class CommandSupport {

    private CommandSupport() {
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static boolean shouldFail(String actualRisk, String threshold) {
        return riskWeight(actualRisk) >= riskWeight(threshold);
    }

    private static int riskWeight(String risk) {
        if (risk == null) {
            return 0;
        }
        switch (risk.trim().toUpperCase()) {
            case "CRITICAL":
                return 4;
            case "ERROR":
                return 3;
            case "WARN":
            case "WARNING":
                return 2;
            case "INFO":
                return 1;
            case "NONE":
            default:
                return 0;
        }
    }
}

