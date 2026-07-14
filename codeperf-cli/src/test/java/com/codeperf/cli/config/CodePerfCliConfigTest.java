package com.codeperf.cli.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodePerfCliConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_LoadAstLocalScanConfig_When_NewYamlProvided() throws Exception {
        Path config = tempDir.resolve(".codeperf.yml");
        Files.write(config, (
                "project: order-service\n"
                        + "staticScan:\n"
                        + "  enabled: true\n"
                        + "  mode: changed\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  includeTests: false\n"
                        + "  baseRef: origin/master\n"
                        + "  headRef: HEAD\n"
                        + "  failOn: WARN\n"
                        + "  callChain:\n"
                        + "    enabled: true\n"
                        + "    maxDepth: 2\n"
                        + "  ioTypes:\n"
                        + "    - mysql\n"
                        + "    - redis\n"
                        + "report:\n"
                        + "  local:\n"
                        + "    enabled: true\n"
                        + "    path: .codeperf/report/custom-source-report.json\n"
                        + "  upload:\n"
                        + "    enabled: true\n"
                        + "    serverUrl: http://codeperf-report.company.com\n").getBytes(StandardCharsets.UTF_8));

        CodePerfCliConfig loaded = CodePerfCliConfig.load(config);

        assertEquals("order-service", loaded.getProject());
        assertTrue(loaded.getStaticScan().isEnabled());
        assertEquals("changed", loaded.getStaticScan().getMode());
        assertEquals("src/main/java", loaded.getStaticScan().getSourceRoots().get(0));
        assertFalse(loaded.getStaticScan().isIncludeTests());
        assertEquals("origin/master", loaded.getStaticScan().getBaseRef());
        assertEquals("HEAD", loaded.getHeadRef());
        assertEquals("HEAD", loaded.getStaticScan().getHeadRef());
        assertEquals("WARN", loaded.getStaticScan().getFailOn());
        assertTrue(loaded.getStaticScan().getCallChain().isEnabled());
        assertEquals(2, loaded.getStaticScan().getCallChain().getMaxDepth());
        assertEquals("mysql", loaded.getStaticScan().getIoTypes().get(0));
        assertEquals("redis", loaded.getStaticScan().getIoTypes().get(1));
        assertTrue(loaded.getReport().getLocal().isEnabled());
        assertEquals(".codeperf/report/custom-source-report.json", loaded.getReport().getLocal().getPath());
        assertTrue(loaded.getReport().getUpload().isEnabled());
        assertEquals("http://codeperf-report.company.com", loaded.getReport().getUpload().getServerUrl());
    }

    @Test
    public void should_ProvideSafeDefaults_When_MinimalYamlProvided() throws Exception {
        Path config = tempDir.resolve(".codeperf.yml");
        Files.write(config, "project: demo\n".getBytes(StandardCharsets.UTF_8));

        CodePerfCliConfig loaded = CodePerfCliConfig.load(config);

        assertEquals("demo", loaded.getProject());
        assertTrue(loaded.getStaticScan().isEnabled());
        assertEquals("changed", loaded.getStaticScan().getMode());
        assertEquals("src/main/java", loaded.getStaticScan().getSourceRoots().get(0));
        assertEquals("origin/master", loaded.getStaticScan().getBaseRef());
        assertEquals("HEAD", loaded.getStaticScan().getHeadRef());
        assertEquals("WARN", loaded.getFailOn());
        assertTrue(loaded.getStaticScan().getCallChain().isEnabled());
        assertEquals(2, loaded.getStaticScan().getCallChain().getMaxDepth());
        assertTrue(loaded.getReport().getLocal().isEnabled());
        assertEquals(".codeperf/report/source-report.json", loaded.getReport().getLocal().getPath());
        assertFalse(loaded.getReport().getUpload().isEnabled());
    }

    @Test
    public void should_MapLegacyServerUrlToReportUpload_When_TopLevelServerUrlProvided() throws Exception {
        Path config = tempDir.resolve(".codeperf.yml");
        Files.write(config, ("project: demo\n"
                + "serverUrl: http://legacy-codeperf.company.com\n").getBytes(StandardCharsets.UTF_8));

        CodePerfCliConfig loaded = CodePerfCliConfig.load(config);

        assertEquals("http://legacy-codeperf.company.com", loaded.getReport().getUpload().getServerUrl());
    }
}
