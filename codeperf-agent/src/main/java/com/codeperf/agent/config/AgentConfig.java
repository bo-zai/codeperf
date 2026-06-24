package com.codeperf.agent.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 运行参数。由 premain/agentmain 的 args 字符串解析而来，格式：key=value;key=value。
 * 字段含义见 docs/02-agent-core.md 第 3 节。
 */
public class AgentConfig {

    private List<String> targetPackages = new ArrayList<>();
    private String entryMethod = "GET";   // HTTP method，大写
    private String entryPath = "/";        // HTTP path 前缀
    private long slowSqlMs = 500;
    private String output = "perf-data.raw";
    private long sampleMs = 10;
    private String mode = "session";

    public static AgentConfig parse(String args) {
        AgentConfig cfg = new AgentConfig();
        if (args == null || args.trim().isEmpty()) {
            return cfg;
        }
        Map<String, String> kv = new HashMap<>();
        for (String pair : args.split(";")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                kv.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
            }
        }
        if (kv.containsKey("targetPackage")) {
            cfg.targetPackages = new ArrayList<>(Arrays.asList(kv.get("targetPackage").split(",")));
        }
        if (kv.containsKey("entry")) {
            // 形如 "POST /api/orders/report"
            String entry = kv.get("entry").trim();
            int sp = entry.indexOf(' ');
            if (sp > 0) {
                cfg.entryMethod = entry.substring(0, sp).trim().toUpperCase();
                cfg.entryPath = entry.substring(sp + 1).trim();
            } else {
                cfg.entryPath = entry;
            }
        }
        if (kv.containsKey("slowSqlMs")) {
            cfg.slowSqlMs = Long.parseLong(kv.get("slowSqlMs"));
        }
        if (kv.containsKey("output")) {
            cfg.output = kv.get("output");
        }
        if (kv.containsKey("sampleMs")) {
            cfg.sampleMs = Long.parseLong(kv.get("sampleMs"));
        }
        if (kv.containsKey("mode")) {
            cfg.mode = kv.get("mode");
        }
        return cfg;
    }

    public List<String> getTargetPackages() {
        return Collections.unmodifiableList(targetPackages);
    }

    public String getEntryMethod() {
        return entryMethod;
    }

    public String getEntryPath() {
        return entryPath;
    }

    public long getSlowSqlMs() {
        return slowSqlMs;
    }

    public String getOutput() {
        return output;
    }

    public long getSampleMs() {
        return sampleMs;
    }

    public String getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "AgentConfig{targetPackages=" + targetPackages
                + ", entry=" + entryMethod + " " + entryPath
                + ", slowSqlMs=" + slowSqlMs
                + ", output=" + output
                + ", sampleMs=" + sampleMs
                + ", mode=" + mode + "}";
    }
}
