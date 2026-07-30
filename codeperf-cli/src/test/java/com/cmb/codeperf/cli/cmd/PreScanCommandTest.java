package com.cmb.codeperf.cli.cmd;

import com.beust.jcommander.JCommander;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreScanCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_DetectUntrackedJavaFile_When_PreScanRuns() throws Exception {
        initGitRepo();
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  failOn: WARN\n");
        write("src/main/java/com/acme/UntrackedService.java",
                "package com.acme;\n"
                        + "class UntrackedService {\n"
                        + "  void build(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      new OrderMapper().selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");

        CapturedRun capturedRun = captureStdout(() -> {
            PreScanCommand command = new PreScanCommand();
            command.setWorkingDirectoryForTest(tempDir);
            return command.execute();
        });

        assertEquals(1, capturedRun.exitCode);
        assertTrue(capturedRun.output.contains("[codeperf] 命令=pre-scan"));
        assertTrue(capturedRun.output.contains("范围=worktree + staged + untracked"));
        assertTrue(capturedRun.output.contains("UntrackedService.java"));
        assertTrue(capturedRun.output.contains("阻断风险 LOOP_IO_AMPLIFICATION"));
    }

    private void initGitRepo() throws Exception {
        runGit("init");
        runGit("config", "user.email", "codeperf@example.com");
        runGit("config", "user.name", "CodePerf Test");
        write("README.md", "# demo\n");
        runGit("add", ".");
        runGit("commit", "-m", "init");
    }

    private void write(String file, String content) throws Exception {
        Path path = tempDir.resolve(file);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void runGit(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("git command failed");
        }
    }

    private CapturedRun captureStdout(CommandRunner runner) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8.name()));
            int exitCode = runner.run();
            return new CapturedRun(exitCode, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("执行测试命令失败", e);
        } finally {
            System.setOut(originalOut);
        }
    }

    private interface CommandRunner {
        int run();
    }

    private static class CapturedRun {
        private final int exitCode;
        private final String output;

        private CapturedRun(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
