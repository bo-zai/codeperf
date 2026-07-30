package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.config.AgentConfig;
import com.cmb.codeperf.agent.session.SessionData;
import com.cmb.codeperf.agent.upload.DynamicEvidenceReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecorderTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_StartRequestForConfiguredHttpMethodList_When_EntryMethodContainsMultipleMethods() {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET,POST,PUT");
        config.setEntryPath("/api");
        config.setMode("continuous");
        Recorder.init(config, null, null);

        boolean getStarted = Recorder.tryStartRequest(new MockRequest("GET", "/api/orders"));
        Recorder.finishRequest();
        boolean postStarted = Recorder.tryStartRequest(new MockRequest("POST", "/api/orders"));
        Recorder.finishRequest();
        boolean putStarted = Recorder.tryStartRequest(new MockRequest("PUT", "/api/orders"));
        Recorder.finishRequest();
        boolean deleteStarted = Recorder.tryStartRequest(new MockRequest("DELETE", "/api/orders"));

        assertTrue(getStarted);
        assertTrue(postStarted);
        assertTrue(putStarted);
        assertFalse(deleteStarted);
    }

    @Test
    public void should_KeepSpecificHttpMethodMatching_When_EntryMethodIsSpecific() {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("POST");
        config.setEntryPath("/api");
        config.setMode("continuous");
        Recorder.init(config, null, null);

        boolean getStarted = Recorder.tryStartRequest(new MockRequest("GET", "/api/orders"));
        boolean postStarted = Recorder.tryStartRequest(new MockRequest("POST", "/api/orders"));
        Recorder.finishRequest();

        assertFalse(getStarted);
        assertTrue(postStarted);
    }

    @Test
    public void should_WriteAndReportEachRequest_When_ModeIsContinuous() {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET");
        config.setEntryPath("/api");
        config.setMode("continuous");
        CountingSessionWriter writer = new CountingSessionWriter(tempDir.resolve("perf-data.raw").toString());
        CountingEvidenceReporter reporter = new CountingEvidenceReporter();
        Recorder.init(config, null, writer, reporter);

        boolean firstStarted = Recorder.tryStartRequest(new MockRequest("GET", "/api/orders/1"));
        Recorder.finishRequest();
        boolean secondStarted = Recorder.tryStartRequest(new MockRequest("GET", "/api/orders/2"));
        Recorder.finishRequest();

        assertTrue(firstStarted);
        assertTrue(secondStarted);
        assertEquals(2, writer.getWriteCount());
        assertEquals(2, reporter.getReportCount());
        assertEquals(2, reporter.getLastRequestCount());
    }

    public static class MockRequest {
        private final String method;
        private final String uri;

        public MockRequest(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        public String getMethod() {
            return method;
        }

        public String getRequestURI() {
            return uri;
        }
    }

    private static class CountingSessionWriter extends SessionWriter {

        private int writeCount;

        CountingSessionWriter(String outputPath) {
            super(outputPath);
        }

        @Override
        public synchronized void write(SessionData session) {
            writeCount++;
        }

        int getWriteCount() {
            return writeCount;
        }
    }

    private static class CountingEvidenceReporter extends DynamicEvidenceReporter {

        private int reportCount;
        private int lastRequestCount;

        CountingEvidenceReporter() {
            super(null);
        }

        @Override
        public void report(SessionData session) throws IOException {
            reportCount++;
            lastRequestCount = session.getRequests().size();
        }

        int getReportCount() {
            return reportCount;
        }

        int getLastRequestCount() {
            return lastRequestCount;
        }
    }
}
