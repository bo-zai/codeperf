package com.codeperf.agent.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_LoadYamlConfig_When_AgentArgumentPointsToFile() throws Exception {
        Path config = tempDir.resolve("agent.yml");
        Files.write(config, (
                "serverUrl: http://127.0.0.1:9095\n"
                        + "analysisTaskId: task-1\n"
                        + "uploadEnabled: true\n"
                        + "targetPackages:\n"
                        + "  - com.acme.order\n"
                        + "entry:\n"
                        + "  method: POST\n"
                        + "  path: /api/orders/report\n"
                        + "slowSqlMs: 250\n"
                        + "sampleMs: 20\n"
                        + "mode: continuous\n"
                        + "output: build/codeperf/perf-data.raw\n").getBytes(StandardCharsets.UTF_8));

        AgentConfig loaded = AgentConfig.load("config=" + config);

        assertEquals("http://127.0.0.1:9095", loaded.getServerUrl());
        assertEquals("task-1", loaded.getAnalysisTaskId());
        assertTrue(loaded.isUploadEnabled());
        assertEquals("com.acme.order", loaded.getTargetPackages().get(0));
        assertEquals("POST", loaded.getEntryMethod());
        assertEquals("/api/orders/report", loaded.getEntryPath());
        assertEquals(250, loaded.getSlowSqlMs());
        assertEquals(20, loaded.getSampleMs());
        assertEquals("continuous", loaded.getMode());
        assertEquals("build/codeperf/perf-data.raw", loaded.getOutput());
    }
}
