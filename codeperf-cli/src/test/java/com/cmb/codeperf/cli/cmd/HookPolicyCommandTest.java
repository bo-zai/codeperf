package com.cmb.codeperf.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HookPolicyCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_PrintFalse_When_PrePushPolicyUsesDefaultConfig() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), "project: demo\n".getBytes(StandardCharsets.UTF_8));

        CapturedRun capturedRun = execute("pre-push");

        assertEquals(0, capturedRun.exitCode);
        assertEquals("false", capturedRun.output.trim());
    }

    @Test
    public void should_PrintTrue_When_PrePushPolicyEnablesBlockOnFailure() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), ("project: demo\n"
                + "gitHooks:\n"
                + "  prePush:\n"
                + "    blockOnFailure: true\n").getBytes(StandardCharsets.UTF_8));

        CapturedRun capturedRun = execute("pre-push");

        assertEquals(0, capturedRun.exitCode);
        assertEquals("true", capturedRun.output.trim());
    }

    @Test
    public void should_PrintFalse_When_HookNameUnknown() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"), ("project: demo\n"
                + "gitHooks:\n"
                + "  prePush:\n"
                + "    blockOnFailure: true\n").getBytes(StandardCharsets.UTF_8));

        CapturedRun capturedRun = execute("pre-commit");

        assertEquals(0, capturedRun.exitCode);
        assertEquals("false", capturedRun.output.trim());
    }

    private CapturedRun execute(String hookName) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8.name()));
            HookPolicyCommand command = new HookPolicyCommand();
            command.setWorkingDirectoryForTest(tempDir);
            command.setHooksForTest(hookName);
            int exitCode = command.execute();
            return new CapturedRun(exitCode, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("执行 hook 策略测试失败", e);
        } finally {
            System.setOut(originalOut);
        }
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
