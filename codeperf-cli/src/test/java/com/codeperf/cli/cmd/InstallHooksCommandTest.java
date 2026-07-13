package com.codeperf.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstallHooksCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_InstallPrePushHookAtGitRoot_When_CommandRunsFromSubdirectory() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: demo\n".getBytes(StandardCharsets.UTF_8));
        Path subdir = tempDir.resolve("module/src/main/java");
        Files.createDirectories(subdir);

        InstallHooksCommand command = new InstallHooksCommand();
        command.setWorkingDirectoryForTest(subdir);

        int exitCode = command.execute();

        Path hook = tempDir.resolve(".git/hooks/pre-push");
        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(hook));
        assertTrue(new String(Files.readAllBytes(hook), StandardCharsets.UTF_8).contains("codeperf scan"));
    }
}
