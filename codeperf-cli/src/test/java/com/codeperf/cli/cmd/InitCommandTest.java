package com.codeperf.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InitCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_CreateConfigAndAgentTemplate_When_InitRuns() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf.yml")));
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf/agent.yml")));
    }

    @Test
    public void should_NotOverwriteExistingConfig_When_InitRunsAgain() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createDirectories(tempDir.resolve(".codeperf"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: existing\n".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve(".codeperf/agent.yml"),
                "serverUrl: http://existing\n".getBytes(StandardCharsets.UTF_8));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertEquals("project: existing\n",
                new String(Files.readAllBytes(tempDir.resolve(".codeperf.yml")), StandardCharsets.UTF_8));
        assertEquals("serverUrl: http://existing\n",
                new String(Files.readAllBytes(tempDir.resolve(".codeperf/agent.yml")), StandardCharsets.UTF_8));
    }
}
