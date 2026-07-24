package com.cmb.codeperf.agent.logging;

/**
 * Agent 内部轻量日志工具。
 * 不引入日志框架是为了避免 javaagent 把日志依赖带入目标应用，降低 classpath 冲突风险。
 */
public final class AgentLogger {

    private static final String LOG_PREFIX = "[codeperf]";

    private AgentLogger() {
    }

    public static void info(String message) {
        System.out.println(format(message));
    }

    public static void error(String message) {
        System.err.println(format(message));
    }

    public static String format(String message) {
        return LOG_PREFIX + " " + safe(message);
    }

    private static String safe(String message) {
        return message == null ? "" : message.replace('\n', ' ').replace('\r', ' ');
    }
}
