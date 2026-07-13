package com.codeperf.cli.config;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class CodePerfCliConfig {

    private String serverUrl;
    private String project;
    private String targetPackage;
    private String classesDir;
    private String baseRef = "origin/main";
    private String headRef = "HEAD";
    private String diffMode = "range";
    private String failOn = "ERROR";
    private String env = "ci";
    private List<String> sourceRoots = new ArrayList<>();

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
        config.serverUrl = stringValue(yaml.get("serverUrl"), config.serverUrl);
        config.project = stringValue(yaml.get("project"), config.project);
        config.targetPackage = stringValue(yaml.get("targetPackage"), config.targetPackage);
        config.classesDir = stringValue(yaml.get("classesDir"), config.classesDir);
        config.baseRef = stringValue(yaml.get("baseRef"), config.baseRef);
        config.headRef = stringValue(yaml.get("headRef"), config.headRef);
        config.diffMode = stringValue(yaml.get("diffMode"), config.diffMode);
        config.failOn = stringValue(yaml.get("failOn"), config.failOn);
        config.env = stringValue(yaml.get("env"), config.env);
        Object sourceRoots = yaml.get("sourceRoots");
        if (sourceRoots instanceof List) {
            config.sourceRoots = new ArrayList<>();
            for (Object item : (List<Object>) sourceRoots) {
                if (item != null) {
                    config.sourceRoots.add(item.toString());
                }
            }
        }
        return config;
    }

    public List<String> sourceRootsOrDefault() {
        if (sourceRoots == null || sourceRoots.isEmpty()) {
            return Collections.singletonList("src/main/java");
        }
        return sourceRoots;
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }
}
