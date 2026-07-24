package com.cmb.codeperf.agent.config;

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
                        + "excludedPackages:\n"
                        + "  - com.acme.order.infrastructure\n"
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
        assertTrue(loaded.getExcludedPackages().contains("com.cmb.cjtz"));
        assertTrue(loaded.getExcludedPackages().contains("com.cmb.checkerframework"));
        assertTrue(loaded.getExcludedPackages().contains("com.cmb.bee"));
        assertTrue(loaded.getExcludedPackages().contains("com.cmbchina.ugw"));
        assertTrue(loaded.getExcludedPackages().contains("com.acme.order.infrastructure"));
        assertEquals("POST", loaded.getEntryMethod());
        assertEquals("/api/orders/report", loaded.getEntryPath());
        assertEquals(250, loaded.getSlowSqlMs());
        assertEquals(20, loaded.getSampleMs());
        assertEquals("continuous", loaded.getMode());
        assertEquals("build/codeperf/perf-data.raw", loaded.getOutput());
    }

    @Test
    public void should_LoadBuildInfo_When_ConfigUsesStableCommitIdentity() throws Exception {
        Path buildInfo = tempDir.resolve("build-info.properties");
        Files.write(buildInfo, (
                "remoteUrl=git@gitlab.example.com:demo/demo-app.git\n"
                        + "commit=abc123\n"
                        + "branch=master\n"
                        + "env=dev\n"
                        + "project=demo-app\n"
                        + "appName=demo-app\n"
                        + "authorName=Developer\n"
                        + "authorEmail=developer@example.com\n"
                        + "commitTime=2026-07-17T15:00:00+08:00\n"
                        + "commitMessage=initial commit\n").getBytes(StandardCharsets.UTF_8));
        Path config = tempDir.resolve("agent.yml");
        Files.write(config, (
                "serverUrl: http://127.0.0.1:9095\n"
                        + "appName: demo-app\n"
                        + "env: dev\n"
                        + "uploadEnabled: true\n"
                        + "buildInfoPath: " + buildInfo.toString().replace("\\", "/") + "\n"
                        + "targetPackages:\n"
                        + "  - com.demo\n"
                        + "entry:\n"
                        + "  method: POST\n"
                        + "  path: /api/orders/report\n").getBytes(StandardCharsets.UTF_8));

        AgentConfig loaded = AgentConfig.load(config.toString());

        assertEquals("demo-app", loaded.getAppName());
        assertEquals("dev", loaded.getEnv());
        assertEquals(buildInfo.toString().replace("\\", "/"), loaded.getBuildInfoPath());
        assertEquals("git@gitlab.example.com:demo/demo-app.git", loaded.getRemoteUrl());
        assertEquals("abc123", loaded.getCommit());
        assertEquals("master", loaded.getBranch());
        assertEquals("developer@example.com", loaded.getAuthorEmail());
        assertEquals("initial commit", loaded.getCommitMessage());
    }
}

