package com.cmb.codeperf.cli.cmd;

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

    @Test
    public void should_InstallHookPassingPushRange_When_GitPrePushProvidesRefs() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: demo\n".getBytes(StandardCharsets.UTF_8));

        InstallHooksCommand command = new InstallHooksCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        String content = new String(Files.readAllBytes(tempDir.resolve(".git/hooks/pre-push")), StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertTrue(content.contains("while read local_ref local_sha remote_ref remote_sha"));
        assertTrue(content.contains("CODEPERF_PUSH_OLD_SHA"));
        assertTrue(content.contains("CODEPERF_PUSH_NEW_SHA"));
        assertTrue(content.contains("CODEPERF_PUSH_REMOTE_BRANCH"));
        assertTrue(content.contains("codeperf scan"));
    }

    @Test
    public void should_InstallHookAllowingPushByDefault_When_ScanFails() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: demo\n".getBytes(StandardCharsets.UTF_8));

        InstallHooksCommand command = new InstallHooksCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        String content = new String(Files.readAllBytes(tempDir.resolve(".git/hooks/pre-push")), StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertTrue(content.contains("block_on_failure=$(codeperf hook-policy pre-push)"));
        assertTrue(content.contains("set +e\n  CODEPERF_PUSH_LOCAL_REF"));
        assertTrue(content.contains("run_default_codeperf_scan"));
        assertTrue(content.contains("默认允许推送"));
        assertTrue(content.contains("return 0"));
    }

    @Test
    public void should_NotOverwriteExistingPrePushHook_When_HookAlreadyExists() throws Exception {
        Files.createDirectories(tempDir.resolve(".git/hooks"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: demo\n".getBytes(StandardCharsets.UTF_8));
        Path hook = tempDir.resolve(".git/hooks/pre-push");
        Files.write(hook, "#!/usr/bin/env sh\necho existing\n".getBytes(StandardCharsets.UTF_8));

        InstallHooksCommand command = new InstallHooksCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertEquals("#!/usr/bin/env sh\necho existing\n",
                new String(Files.readAllBytes(hook), StandardCharsets.UTF_8));
    }
}

