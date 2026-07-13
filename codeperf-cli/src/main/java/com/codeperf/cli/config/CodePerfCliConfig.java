package com.codeperf.cli.config;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CodePerfCliConfig {

    private String project;
    private StaticScanConfig staticScan = new StaticScanConfig();
    private AgentOnboardingConfig agent = new AgentOnboardingConfig();
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
        Object agent = yaml.get("agent");
        if (agent instanceof Map) {
            applyAgent(config.agent, (Map<String, Object>) agent);
        }
        Object modules = yaml.get("modules");
        if (modules instanceof List) {
            config.modules = parseModules((List<Object>) modules);
        }
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

    private static void applyAgent(AgentOnboardingConfig config, Map<String, Object> yaml) {
        config.setEnabled(booleanValue(yaml.get("enabled"), config.isEnabled()));
        config.setServerUrl(stringValue(yaml.get("serverUrl"), config.getServerUrl()));
        config.setConfigPath(stringValue(yaml.get("configPath"), config.getConfigPath()));
        config.setJarPath(stringValue(yaml.get("jarPath"), config.getJarPath()));
        config.setTargetPackages(stringList(yaml.get("targetPackages"), config.getTargetPackages()));
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
        String targetPackage = stringValue(yaml.get("targetPackage"), null);
        if (targetPackage != null && config.agent.getTargetPackages().isEmpty()) {
            config.agent.getTargetPackages().add(targetPackage);
        }
        config.agent.setServerUrl(stringValue(yaml.get("serverUrl"), config.agent.getServerUrl()));
    }

    public String getTargetPackage() {
        if (agent.getTargetPackages() == null || agent.getTargetPackages().isEmpty()) {
            return null;
        }
        return agent.getTargetPackages().get(0);
    }

    public String getServerUrl() {
        return agent.getServerUrl();
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
