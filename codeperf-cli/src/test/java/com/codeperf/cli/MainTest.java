package com.codeperf.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainTest {

    @Test
    public void should_PrintGlobalUsageAndReturnSuccess_When_GlobalHelpRequested() {
        CapturedRun capturedRun = captureStdout(() -> Main.run(new String[]{"--help"}));

        assertEquals(0, capturedRun.exitCode);
        assertTrue(capturedRun.output.contains("Usage: codeperf"));
        assertTrue(capturedRun.output.contains("scan"));
        assertTrue(capturedRun.output.contains("install-hooks"));
    }

    @Test
    public void should_PrintGlobalUsageAndReturnSuccess_When_GlobalShortHelpRequested() {
        CapturedRun capturedRun = captureStdout(() -> Main.run(new String[]{"-h"}));

        assertEquals(0, capturedRun.exitCode);
        assertTrue(capturedRun.output.contains("Usage: codeperf"));
    }

    @Test
    public void should_PrintCommandUsageAndReturnSuccess_When_ScanHelpRequested() {
        CapturedRun capturedRun = captureStdout(() -> Main.run(new String[]{"scan", "--help"}));

        assertEquals(0, capturedRun.exitCode);
        assertTrue(capturedRun.output.contains("Usage: codeperf scan"));
        assertTrue(capturedRun.output.contains("--all"));
        assertTrue(capturedRun.output.contains("--output"));
    }

    @Test
    public void should_ReturnFailure_When_UnknownCommandProvided() {
        CapturedRun capturedRun = captureStdout(() -> Main.run(new String[]{"unknown"}));

        assertEquals(1, capturedRun.exitCode);
        assertTrue(capturedRun.output.contains("Usage: codeperf"));
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
