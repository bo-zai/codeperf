package com.codeperf.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoctorCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_ReturnZero_When_ConfigAndAgentTemplateExist() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve(".codeperf"));
        Files.write(tempDir.resolve(".codeperf.yml"),
                ("project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "agent:\n"
                        + "  configPath: .codeperf/agent.yml\n").getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve(".codeperf/agent.yml"),
                "serverUrl: http://codeperf.company.com\n".getBytes(StandardCharsets.UTF_8));

        DoctorCommand command = new DoctorCommand();
        command.setWorkingDirectoryForTest(tempDir);

        assertEquals(0, command.execute());
    }
}
