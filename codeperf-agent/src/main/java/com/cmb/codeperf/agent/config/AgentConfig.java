package com.cmb.codeperf.agent.config;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Agent 运行参数。正式环境由 -javaagent 参数指向 agent.yml 配置文件。
 * 字段含义见 docs/02-agent-core.md 第 3 节。
 */
@Data
public class AgentConfig {

    private static final List<String> DEFAULT_EXCLUDED_PACKAGES = Arrays.asList(
            "com.cmb.cjtz",
            "com.cmb.checkerframework",
            "com.cmb.bee",
            "com.cmbchina.ugw");

    private List<String> targetPackages = new ArrayList<>();
    private List<String> excludedPackages = defaultExcludedPackages();
    private String entryMethod = "GET";   // HTTP method，大写
    private String entryPath = "/";        // HTTP path 前缀
    private long slowSqlMs = 500;
    private String output = "perf-data.raw";
    private long sampleMs = 10;
    private String mode = "session";
    private String serverUrl;
    private String appName;
    private String env = "dev";
    private String buildInfoPath = "build-info.properties";
    private String remoteUrl;
    private String commit;
    private String branch;
    private String project;
    private String authorName;
    private String authorEmail;
    private String commitTime;
    private String commitMessage;
    private String analysisTaskId;
    private boolean uploadEnabled;

    /**
     * 加载 Agent 配置。正式环境只建议传入 config=/path/agent.yml 或直接传入配置文件路径。
     *
     * @param args premain 参数
     * @return Agent 配置
     * @throws IOException 配置文件不可读时抛出
     */
    public static AgentConfig load(String args) throws IOException {
        String configPath = resolveConfigPath(args);
        if (configPath == null) {
            return parse(args);
        }
        try (InputStream input = Files.newInputStream(Paths.get(configPath))) {
            Map<String, Object> yaml = new Yaml().load(input);
            return fromYaml(yaml);
        }
    }

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
        if (kv.containsKey("excludedPackage")) {
            cfg.excludedPackages = mergeExcludedPackages(Arrays.asList(kv.get("excludedPackage").split(",")));
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
        if (kv.containsKey("serverUrl")) {
            cfg.serverUrl = kv.get("serverUrl");
        }
        if (kv.containsKey("analysisTaskId")) {
            cfg.analysisTaskId = kv.get("analysisTaskId");
        }
        if (kv.containsKey("uploadEnabled")) {
            cfg.uploadEnabled = Boolean.parseBoolean(kv.get("uploadEnabled"));
        }
        return cfg;
    }

    private static String resolveConfigPath(String args) {
        if (args == null || args.trim().isEmpty()) {
            return null;
        }
        String value = args.trim();
        if (value.startsWith("config=")) {
            return value.substring("config=".length()).trim();
        }
        if (!value.contains("=") && (value.endsWith(".yml") || value.endsWith(".yaml"))) {
            return value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static AgentConfig fromYaml(Map<String, Object> yaml) {
        AgentConfig cfg = new AgentConfig();
        if (yaml == null) {
            return cfg;
        }
        Object targetPackages = yaml.get("targetPackages");
        if (targetPackages instanceof List) {
            cfg.targetPackages = new ArrayList<>();
            for (Object item : (List<Object>) targetPackages) {
                if (item != null) {
                    cfg.targetPackages.add(item.toString());
                }
            }
        }
        Object excludedPackages = yaml.get("excludedPackages");
        if (excludedPackages instanceof List) {
            List<String> values = new ArrayList<>();
            for (Object item : (List<Object>) excludedPackages) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
            cfg.excludedPackages = mergeExcludedPackages(values);
        }
        Object entry = yaml.get("entry");
        if (entry instanceof Map) {
            Map<String, Object> entryMap = (Map<String, Object>) entry;
            Object method = entryMap.get("method");
            Object path = entryMap.get("path");
            if (method != null) {
                cfg.entryMethod = method.toString().toUpperCase();
            }
            if (path != null) {
                cfg.entryPath = path.toString();
            }
        }
        cfg.slowSqlMs = longValue(yaml.get("slowSqlMs"), cfg.slowSqlMs);
        cfg.sampleMs = longValue(yaml.get("sampleMs"), cfg.sampleMs);
        cfg.mode = stringValue(yaml.get("mode"), cfg.mode);
        cfg.output = stringValue(yaml.get("output"), cfg.output);
        cfg.serverUrl = stringValue(yaml.get("serverUrl"), cfg.serverUrl);
        cfg.appName = stringValue(yaml.get("appName"), cfg.appName);
        cfg.env = stringValue(yaml.get("env"), cfg.env);
        cfg.buildInfoPath = stringValue(yaml.get("buildInfoPath"), cfg.buildInfoPath);
        cfg.analysisTaskId = stringValue(yaml.get("analysisTaskId"), cfg.analysisTaskId);
        cfg.uploadEnabled = booleanValue(yaml.get("uploadEnabled"), cfg.uploadEnabled);
        cfg.loadBuildInfo();
        return cfg;
    }

    private void loadBuildInfo() {
        if (buildInfoPath == null || buildInfoPath.trim().isEmpty()) {
            return;
        }
        try {
            Properties properties = new Properties();
            try (Reader reader = new StringReader(new String(
                    Files.readAllBytes(Paths.get(buildInfoPath)), StandardCharsets.UTF_8))) {
                properties.load(reader);
            }
            remoteUrl = trimToNull(properties.getProperty("remoteUrl"));
            commit = trimToNull(properties.getProperty("commit"));
            branch = trimToNull(properties.getProperty("branch"));
            project = trimToNull(properties.getProperty("project"));
            appName = firstNonBlank(appName, properties.getProperty("appName"));
            env = firstNonBlank(env, properties.getProperty("env"));
            authorName = trimToNull(properties.getProperty("authorName"));
            authorEmail = trimToNull(properties.getProperty("authorEmail"));
            commitTime = trimToNull(properties.getProperty("commitTime"));
            commitMessage = trimToNull(properties.getProperty("commitMessage"));
        } catch (IOException ignore) {
            // build-info 只是动态上报增强信息；缺失时不影响 agent 启动和采集主链路。
        }
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    private static long longValue(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return trimToNull(secondary);
    }

    private static List<String> defaultExcludedPackages() {
        return new ArrayList<>(DEFAULT_EXCLUDED_PACKAGES);
    }

    private static List<String> mergeExcludedPackages(List<String> configuredPackages) {
        Set<String> merged = new LinkedHashSet<>();
        for (String value : DEFAULT_EXCLUDED_PACKAGES) {
            addPackagePrefix(merged, value);
        }
        if (configuredPackages != null) {
            for (String value : configuredPackages) {
                addPackagePrefix(merged, value);
            }
        }
        return new ArrayList<>(merged);
    }

    private static void addPackagePrefix(Set<String> values, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            values.add(trimmed);
        }
    }
}

