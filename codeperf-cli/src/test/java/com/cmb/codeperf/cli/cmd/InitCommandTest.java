package com.cmb.codeperf.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InitCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_CreateCliConfigOnly_When_InitRuns() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf.yml")));
        assertFalse(Files.exists(tempDir.resolve(".codeperf/agent.yml")));
        String config = configText(tempDir);
        assertTrue(config.contains("report:\n"));
        assertTrue(config.contains("    path: .codeperf/report/source-report.json\n"));
        assertTrue(config.contains("    enabled: false\n"));
        assertFalse(config.contains("agent:\n"));
    }

    @Test
    public void should_InferProjectNameFromGitOrigin_When_RemoteUrlExists() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        writeGitOrigin("git@gitee.com:dengquanbo/music-education-app.git");

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        String config = new String(Files.readAllBytes(tempDir.resolve(".codeperf.yml")), StandardCharsets.UTF_8);
        assertTrue(config.contains("project: music-education-app\n"));
    }

    @Test
    public void should_FallbackToGitRootDirectoryName_When_RemoteUrlMissing() throws Exception {
        Path projectRoot = tempDir.resolve("local-project");
        Files.createDirectories(projectRoot.resolve(".git"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(projectRoot);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        String config = new String(Files.readAllBytes(projectRoot.resolve(".codeperf.yml")), StandardCharsets.UTF_8);
        assertTrue(config.contains("project: local-project\n"));
    }

    @Test
    public void should_WriteConfigToGitRoot_When_InitRunsFromSubDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createDirectories(tempDir.resolve("mall-admin/src/main/java"));
        writeGitOrigin("https://github.com/macrozheng/mall.git");

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir.resolve("mall-admin/src/main/java"));

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf.yml")));
        assertFalse(Files.exists(tempDir.resolve(".codeperf/agent.yml")));
        assertTrue(configText(tempDir).contains("project: mall\n"));
    }

    @Test
    public void should_InitSingleSourceRoot_When_SingleModuleProject() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.createDirectories(tempDir.resolve("src/test/java/com/example"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        String config = configText(tempDir);
        assertTrue(config.contains("  sourceRoots:\n    - src/main/java\n"));
        assertFalse(config.contains("src/test/java"));
        assertFalse(config.contains("modules:\n"));
    }

    @Test
    public void should_InitSourceRootsAndModules_When_MultiModuleProject() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createDirectories(tempDir.resolve("admin/src/main/java/com/example/admin"));
        Files.createDirectories(tempDir.resolve("app/src/main/java/com/example/app"));
        Files.createDirectories(tempDir.resolve("common/src/main/java/com/example/common"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        String config = configText(tempDir);
        assertTrue(config.contains("    - admin/src/main/java\n"));
        assertTrue(config.contains("    - app/src/main/java\n"));
        assertTrue(config.contains("    - common/src/main/java\n"));
        assertTrue(config.contains("modules:\n"));
        assertTrue(config.contains("  - name: admin\n"));
        assertTrue(config.contains("  - name: app\n"));
        assertTrue(config.contains("  - name: common\n"));
    }

    @Test
    public void should_IgnoreGeneratedAndHiddenDirectories_When_DiscoveringSourceRoots() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createDirectories(tempDir.resolve("app/src/main/java"));
        Files.createDirectories(tempDir.resolve("target/generated/src/main/java"));
        Files.createDirectories(tempDir.resolve("build/generated/src/main/java"));
        Files.createDirectories(tempDir.resolve(".gradle/cache/src/main/java"));
        Files.createDirectories(tempDir.resolve("node_modules/demo/src/main/java"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        String config = configText(tempDir);
        assertTrue(config.contains("    - app/src/main/java\n"));
        assertFalse(config.contains("target/generated"));
        assertFalse(config.contains("build/generated"));
        assertFalse(config.contains(".gradle"));
        assertFalse(config.contains("node_modules"));
    }

    @Test
    public void should_FallbackToDefaultSourceRoot_When_NoJavaSourceRootFound() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertTrue(configText(tempDir).contains("  sourceRoots:\n    - src/main/java\n"));
    }

    @Test
    public void should_NotOverwriteExistingConfig_When_InitRunsAgain() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: existing\n".getBytes(StandardCharsets.UTF_8));

        InitCommand command = new InitCommand();
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(0, exitCode);
        assertEquals("project: existing\n",
                new String(Files.readAllBytes(tempDir.resolve(".codeperf.yml")), StandardCharsets.UTF_8));
        assertFalse(Files.exists(tempDir.resolve(".codeperf/agent.yml")));
    }

    @Test
    public void should_PrintInstallHooksHint_When_InitCompleted() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));

        String output = captureStdout(() -> {
            InitCommand command = new InitCommand();
            command.setWorkingDirectoryForTest(tempDir);
            return command.execute();
        });

        assertTrue(output.contains("codeperf install-hooks"));
    }

    private void writeGitOrigin(String remoteUrl) throws Exception {
        Files.write(tempDir.resolve(".git/config"), ("[remote \"origin\"]\n"
                + "    url = " + remoteUrl + "\n"
                + "    fetch = +refs/heads/*:refs/remotes/origin/*\n").getBytes(StandardCharsets.UTF_8));
    }

    private String configText(Path root) throws Exception {
        return new String(Files.readAllBytes(root.resolve(".codeperf.yml")), StandardCharsets.UTF_8);
    }

    private String captureStdout(CommandAction action) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8.name())) {
            System.setOut(stream);
            action.execute();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private interface CommandAction {
        int execute() throws Exception;
    }
}

