package com.codeperf.cli.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CodePerfCliConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_LoadConfig_When_CodePerfYamlProvided() throws Exception {
        Path config = tempDir.resolve(".codeperf.yml");
        Files.write(config, (
                "serverUrl: http://127.0.0.1:9090\n"
                        + "project: codeperf-demo\n"
                        + "targetPackage: com.codeperf.demo\n"
                        + "classesDir: codeperf-demo/target/classes\n"
                        + "baseRef: origin/main\n"
                        + "headRef: HEAD\n"
                        + "diffMode: range\n"
                        + "failOn: WARN\n"
                        + "sourceRoots:\n"
                        + "  - codeperf-demo/src/main/java\n").getBytes(StandardCharsets.UTF_8));

        CodePerfCliConfig loaded = CodePerfCliConfig.load(config);

        assertEquals("http://127.0.0.1:9090", loaded.getServerUrl());
        assertEquals("codeperf-demo", loaded.getProject());
        assertEquals("com.codeperf.demo", loaded.getTargetPackage());
        assertEquals("codeperf-demo/target/classes", loaded.getClassesDir());
        assertEquals("origin/main", loaded.getBaseRef());
        assertEquals("HEAD", loaded.getHeadRef());
        assertEquals("range", loaded.getDiffMode());
        assertEquals("WARN", loaded.getFailOn());
        assertEquals("codeperf-demo/src/main/java", loaded.getSourceRoots().get(0));
    }
}
