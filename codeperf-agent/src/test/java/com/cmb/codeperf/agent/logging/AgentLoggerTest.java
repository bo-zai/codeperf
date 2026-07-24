package com.cmb.codeperf.agent.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentLoggerTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void should_WriteInfoLogWithCodePerfPrefix_When_InfoCalled() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8.name()));

        AgentLogger.info("agent injection succeeded");

        assertTrue(output.toString(StandardCharsets.UTF_8.name())
                .contains("[codeperf] agent injection succeeded"));
    }

    @Test
    public void should_WriteErrorLogWithCodePerfPrefix_When_ErrorCalled() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setErr(new PrintStream(output, true, StandardCharsets.UTF_8.name()));

        AgentLogger.error("dynamic evidence upload failed");

        assertTrue(output.toString(StandardCharsets.UTF_8.name())
                .contains("[codeperf] dynamic evidence upload failed"));
    }
}
