package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.collect.advice.MybatisMapperProxyAdvice;
import com.cmb.codeperf.agent.config.AgentConfig;
import com.cmb.codeperf.agent.session.SessionData;
import com.cmb.codeperf.agent.upload.DynamicEvidenceReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

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
        assertTrue(waitUntil(() -> writer.getWriteCount() == 2));
        assertTrue(waitUntil(() -> reporter.getReportCount() == 2));
        assertEquals(2, reporter.getLastRequestCount());
    }

    @Test
    public void should_ReturnImmediately_When_SessionWriterIsBlocked() throws Exception {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET");
        config.setEntryPath("/api");
        config.setMode("continuous");
        BlockingSessionWriter writer = new BlockingSessionWriter(tempDir.resolve("perf-data.raw").toString());
        Recorder.init(config, null, writer, null);

        assertTrue(Recorder.tryStartRequest(new MockRequest("GET", "/api/orders/1")));
        long startNanos = System.nanoTime();
        Recorder.finishRequest();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(elapsedMs < 200, "finishRequest must not wait for local file writing");
        assertTrue(writer.awaitWriteStarted(), "background writer should receive session");
        assertEquals(0, writer.getWriteCount());

        writer.release();
        assertTrue(writer.awaitWriteCompleted(), "background writer should finish after release");
        assertEquals(1, writer.getWriteCount());
    }

    @Test
    public void should_ReturnImmediately_When_DynamicReporterIsBlocked() throws Exception {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET");
        config.setEntryPath("/api");
        config.setMode("continuous");
        BlockingEvidenceReporter reporter = new BlockingEvidenceReporter();
        Recorder.init(config, null, null, reporter);

        assertTrue(Recorder.tryStartRequest(new MockRequest("GET", "/api/orders/1")));
        long startNanos = System.nanoTime();
        Recorder.finishRequest();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(elapsedMs < 200, "finishRequest must not wait for dynamic evidence upload");
        assertTrue(reporter.awaitReportStarted(), "background reporter should receive session");
        assertEquals(0, reporter.getReportCount());

        reporter.release();
        assertTrue(reporter.awaitReportCompleted(), "background reporter should finish after release");
        assertEquals(1, reporter.getReportCount());
    }

    @Test
    public void should_WriteCompletionLog_When_AsyncSessionPublishSucceeded() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8.name()));
        try {
            AgentConfig config = new AgentConfig();
            config.setEntryMethod("GET");
            config.setEntryPath("/api");
            config.setMode("continuous");
            CountingSessionWriter writer = new CountingSessionWriter(tempDir.resolve("perf-data.raw").toString());
            CountingEvidenceReporter reporter = new CountingEvidenceReporter();
            Recorder.init(config, null, writer, reporter);

            assertTrue(Recorder.tryStartRequest(new MockRequest("GET", "/api/orders/1")));
            Recorder.recordMybatis("com.cmb.demo.OrderMapper.selectById", "SELECT", 5L);
            Recorder.finishRequest();

            assertTrue(waitUntil(() -> logs(output)
                    .contains("session async publish completed")));
            String logs = logs(output);
            assertTrue(logs.contains("requests=1"));
            assertTrue(logs.contains("ioEvents=1"));
            assertTrue(logs.contains("localWritten=true"));
            assertTrue(logs.contains("uploaded=true"));
            assertTrue(logs.contains("queueRemaining="));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void should_RecordIoEvents_When_RuntimeFrameworkCallsHappenInsideRequest() {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET");
        config.setEntryPath("/api");
        config.setMode("continuous");
        CountingEvidenceReporter reporter = new CountingEvidenceReporter();
        Recorder.init(config, null, null, reporter);

        assertTrue(Recorder.tryStartRequest(new MockRequest("GET", "/api/orders")));
        Recorder.enterMethod("com.cmb.demo.OrderService.preview");
        Recorder.recordMybatis("com.cmb.demo.OrderMapper.selectById", "SELECT", 5L);
        Recorder.recordHttp("SPRING_REST_TEMPLATE", "getForObject", "http://inventory/api/items", 8L);
        Recorder.recordRpc("DUBBO", "com.cmb.demo.InventoryFacade", "queryStock", 13L);
        Recorder.finishRequest();

        assertTrue(waitUntil(() -> reporter.getLastIoEventCount() == 3));
        assertEquals("getForObject", reporter.getLastHttpMethodName());
    }

    @Test
    public void should_AggregateSameIoEvent_When_RuntimeFrameworkCallRepeatsInsideRequest() {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET");
        config.setEntryPath("/api");
        config.setMode("continuous");
        CountingEvidenceReporter reporter = new CountingEvidenceReporter();
        Recorder.init(config, null, null, reporter);

        assertTrue(Recorder.tryStartRequest(new MockRequest("GET", "/api/orders")));
        Recorder.enterMethod("com.cmb.demo.OrderService.preview");
        Recorder.recordMybatis("com.cmb.demo.OrderMapper.selectById", "SELECT", 5L);
        Recorder.recordMybatis("com.cmb.demo.OrderMapper.selectById", "SELECT", 8L);
        Recorder.recordMybatis("com.cmb.demo.OrderMapper.selectById", "SELECT", 13L);
        Recorder.finishRequest();

        assertTrue(waitUntil(() -> reporter.getLastIoEventCount() == 1));
        assertEquals(3, reporter.getLastIoEventRepeatCount());
        assertEquals(26L, reporter.getLastIoEventElapsedMs());
    }

    @Test
    public void should_RecordMybatisMapperMethod_When_MapperProxyInvokeAdviceRuns() throws NoSuchMethodException {
        AgentConfig config = new AgentConfig();
        config.setEntryMethod("GET");
        config.setEntryPath("/api");
        config.setMode("continuous");
        CountingEvidenceReporter reporter = new CountingEvidenceReporter();
        Recorder.init(config, null, null, reporter);
        Method method = DemoOrderMapper.class.getMethod("selectByUserId", Long.class);

        assertTrue(Recorder.tryStartRequest(new MockRequest("GET", "/api/orders")));
        MybatisMapperProxyAdvice.exit(method, System.nanoTime());
        Recorder.finishRequest();

        assertTrue(waitUntil(() -> reporter.getLastIoEventCount() == 1));
        assertEquals("selectByUserId", reporter.getLastIoEventMethodName());
    }

    private static boolean waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    private static String logs(ByteArrayOutputStream output) {
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
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

    public interface DemoOrderMapper {
        Object selectByUserId(Long userId);
    }

    private static class CountingSessionWriter extends SessionWriter {

        private final AtomicInteger writeCount = new AtomicInteger();

        CountingSessionWriter(String outputPath) {
            super(outputPath);
        }

        @Override
        public synchronized boolean write(SessionData session) {
            writeCount.incrementAndGet();
            return true;
        }

        int getWriteCount() {
            return writeCount.get();
        }
    }

    private static class BlockingSessionWriter extends SessionWriter {

        private final CountDownLatch writeStarted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch writeCompleted = new CountDownLatch(1);
        private final AtomicInteger writeCount = new AtomicInteger();

        BlockingSessionWriter(String outputPath) {
            super(outputPath);
        }

        @Override
        public synchronized boolean write(SessionData session) {
            writeStarted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            writeCount.incrementAndGet();
            writeCompleted.countDown();
            return true;
        }

        boolean awaitWriteStarted() throws InterruptedException {
            return writeStarted.await(2, TimeUnit.SECONDS);
        }

        void release() {
            release.countDown();
        }

        boolean awaitWriteCompleted() throws InterruptedException {
            return writeCompleted.await(2, TimeUnit.SECONDS);
        }

        int getWriteCount() {
            return writeCount.get();
        }
    }

    private static class CountingEvidenceReporter extends DynamicEvidenceReporter {

        private final AtomicInteger reportCount = new AtomicInteger();
        private volatile int lastRequestCount;
        private volatile int lastIoEventCount;
        private volatile int lastIoEventRepeatCount;
        private volatile long lastIoEventElapsedMs;
        private volatile String lastIoEventMethodName;
        private volatile String lastHttpMethodName;

        CountingEvidenceReporter() {
            super(null);
        }

        @Override
        public void report(SessionData session) throws IOException {
            reportCount.incrementAndGet();
            lastRequestCount = session.getRequests().size();
            lastIoEventCount = session.getRequests().isEmpty()
                    ? 0
                    : session.getRequests().get(session.getRequests().size() - 1).getIoEvents().size();
            if (!session.getRequests().isEmpty()
                    && !session.getRequests().get(session.getRequests().size() - 1).getIoEvents().isEmpty()) {
                lastIoEventRepeatCount = session.getRequests().get(session.getRequests().size() - 1)
                        .getIoEvents().get(0).getCount();
                lastIoEventElapsedMs = session.getRequests().get(session.getRequests().size() - 1)
                        .getIoEvents().get(0).getElapsedMs();
                lastIoEventMethodName = session.getRequests().get(session.getRequests().size() - 1)
                        .getIoEvents().get(0).getMethodName();
                for (int i = 0; i < session.getRequests().get(session.getRequests().size() - 1).getIoEvents().size(); i++) {
                    if ("HTTP".equals(session.getRequests().get(session.getRequests().size() - 1)
                            .getIoEvents().get(i).getIoType())) {
                        lastHttpMethodName = session.getRequests().get(session.getRequests().size() - 1)
                                .getIoEvents().get(i).getMethodName();
                    }
                }
            }
        }

        int getReportCount() {
            return reportCount.get();
        }

        int getLastRequestCount() {
            return lastRequestCount;
        }

        int getLastIoEventCount() {
            return lastIoEventCount;
        }

        int getLastIoEventRepeatCount() {
            return lastIoEventRepeatCount;
        }

        long getLastIoEventElapsedMs() {
            return lastIoEventElapsedMs;
        }

        String getLastIoEventMethodName() {
            return lastIoEventMethodName;
        }

        String getLastHttpMethodName() {
            return lastHttpMethodName;
        }
    }

    private static class BlockingEvidenceReporter extends DynamicEvidenceReporter {

        private final CountDownLatch reportStarted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch reportCompleted = new CountDownLatch(1);
        private final AtomicInteger reportCount = new AtomicInteger();

        BlockingEvidenceReporter() {
            super(null);
        }

        @Override
        public void report(SessionData session) throws IOException {
            reportStarted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            reportCount.incrementAndGet();
            reportCompleted.countDown();
        }

        boolean awaitReportStarted() throws InterruptedException {
            return reportStarted.await(2, TimeUnit.SECONDS);
        }

        void release() {
            release.countDown();
        }

        boolean awaitReportCompleted() throws InterruptedException {
            return reportCompleted.await(2, TimeUnit.SECONDS);
        }

        int getReportCount() {
            return reportCount.get();
        }
    }
}
