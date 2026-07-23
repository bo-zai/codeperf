package com.cmb.codeperf.cli.config;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CLI 顶层配置：加载 {@code .codeperf.yml} 并管理扫描、报告、模块等子配置。
 * <p>
 * 配置结构：
 * <pre>
 * project: my-app
 * staticScan:
 *   enabled: true
 *   mode: changed
 *   sourceRoots: [src/main/java]
 * report:
 *   local: {enabled: true, path: .codeperf/report.json}
 *   upload: {enabled: false, serverUrl: http://codeperf.company.com}
 * modules:
 *   - name: user-service
 *     sourceRoots: [user-service/src/main/java]
 * </pre>
 * <p>
 * 设计决策：
 * <ul>
 *   <li>向后兼容：顶层字段（baseRef、failOn 等）仍支持，自动迁移到 staticScan 子配置</li>
 *   <li>默认值：所有配置项均有合理默认值，确保最小配置即可运行</li>
 * </ul>
 */
@Data
public class CodePerfCliConfig {

    private String project;
    private StaticScanConfig staticScan = new StaticScanConfig();
    private ReportConfig report = new ReportConfig();
    private List<ModuleScanConfig> modules = new ArrayList<>();
    private String classesDir;
    private String env = "local";

    public static CodePerfCliConfig load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            Map<String, Object> yaml = new Yaml().load(input);
            return fromYaml(yaml);
        }
    }

    @SuppressWarnings("unchecked")
    private static CodePerfCliConfig fromYaml(Map<String, Object> yaml) {
        CodePerfCliConfig config = new CodePerfCliConfig();
        if (yaml == null) {
            return config;
        }
        config.project = stringValue(yaml.get("project"), config.project);
        config.classesDir = stringValue(yaml.get("classesDir"), config.classesDir);
        config.env = stringValue(yaml.get("env"), config.env);
        Object staticScan = yaml.get("staticScan");
        if (staticScan instanceof Map) {
            applyStaticScan(config.staticScan, (Map<String, Object>) staticScan);
        }
        Object report = yaml.get("report");
        if (report instanceof Map) {
            applyReport(config.report, (Map<String, Object>) report);
        }
        Object modules = yaml.get("modules");
        if (modules instanceof List) {
            config.modules = parseModules((List<Object>) modules);
        }
        // 向后兼容：顶层字段迁移到 staticScan 子配置，支持旧版配置文件
        applyLegacyFields(config, yaml);
        return config;
    }

    @SuppressWarnings("unchecked")
    private static void applyStaticScan(StaticScanConfig config, Map<String, Object> yaml) {
        config.setEnabled(booleanValue(yaml.get("enabled"), config.isEnabled()));
        config.setMode(stringValue(yaml.get("mode"), config.getMode()));
        config.setSourceRoots(stringList(yaml.get("sourceRoots"), config.getSourceRoots()));
        config.setIncludeTests(booleanValue(yaml.get("includeTests"), config.isIncludeTests()));
        config.setBaseRef(stringValue(yaml.get("baseRef"), config.getBaseRef()));
        config.setHeadRef(stringValue(yaml.get("headRef"), config.getHeadRef()));
        config.setFailOn(stringValue(yaml.get("failOn"), config.getFailOn()));
        config.setIoTypes(stringList(yaml.get("ioTypes"), config.getIoTypes()));
        Object callChain = yaml.get("callChain");
        if (callChain instanceof Map) {
            applyCallChain(config.getCallChain(), (Map<String, Object>) callChain);
        }
    }

    private static void applyCallChain(CallChainConfig config, Map<String, Object> yaml) {
        config.setEnabled(booleanValue(yaml.get("enabled"), config.isEnabled()));
        config.setMaxDepth(intValue(yaml.get("maxDepth"), config.getMaxDepth()));
    }

    @SuppressWarnings("unchecked")
    private static void applyReport(ReportConfig config, Map<String, Object> yaml) {
        Object local = yaml.get("local");
        if (local instanceof Map) {
            applyLocalReport(config.getLocal(), (Map<String, Object>) local);
        }
        Object upload = yaml.get("upload");
        if (upload instanceof Map) {
            applyUploadReport(config.getUpload(), (Map<String, Object>) upload);
        }
    }

    private static void applyLocalReport(LocalReportConfig config, Map<String, Object> yaml) {
        config.setEnabled(booleanValue(yaml.get("enabled"), config.isEnabled()));
        config.setPath(stringValue(yaml.get("path"), config.getPath()));
    }

    private static void applyUploadReport(UploadReportConfig config, Map<String, Object> yaml) {
        config.setEnabled(booleanValue(yaml.get("enabled"), config.isEnabled()));
        config.setServerUrl(stringValue(yaml.get("serverUrl"), config.getServerUrl()));
    }

    @SuppressWarnings("unchecked")
    private static List<ModuleScanConfig> parseModules(List<Object> values) {
        List<ModuleScanConfig> modules = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) value;
            ModuleScanConfig module = new ModuleScanConfig();
            module.setName(stringValue(item.get("name"), module.getName()));
            module.setSourceRoots(stringList(item.get("sourceRoots"), module.getSourceRoots()));
            module.setTargetPackages(stringList(item.get("targetPackages"), module.getTargetPackages()));
            modules.add(module);
        }
        return modules;
    }

    private static void applyLegacyFields(CodePerfCliConfig config, Map<String, Object> yaml) {
        config.staticScan.setBaseRef(stringValue(yaml.get("baseRef"), config.staticScan.getBaseRef()));
        config.staticScan.setHeadRef(stringValue(yaml.get("headRef"), config.staticScan.getHeadRef()));
        config.staticScan.setFailOn(stringValue(yaml.get("failOn"), config.staticScan.getFailOn()));
        config.staticScan.setSourceRoots(stringList(yaml.get("sourceRoots"), config.staticScan.getSourceRoots()));
        String serverUrl = stringValue(yaml.get("serverUrl"), null);
        if (serverUrl != null && isBlank(config.report.getUpload().getServerUrl())) {
            config.report.getUpload().setServerUrl(serverUrl);
        }
    }

    public String getBaseRef() {
        return staticScan.getBaseRef();
    }

    public String getHeadRef() {
        return staticScan.getHeadRef();
    }

    public String getDiffMode() {
        return staticScan.getMode();
    }

    public String getFailOn() {
        return staticScan.getFailOn();
    }

    public List<String> sourceRootsOrDefault() {
        return staticScan.getSourceRoots();
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }

    private static int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value, List<String> defaultValue) {
        if (!(value instanceof List)) {
            return defaultValue;
        }
        List<String> values = new ArrayList<>();
        for (Object item : (List<Object>) value) {
            if (item != null) {
                values.add(item.toString());
            }
        }
        return values;
    }
}

