package com.cmb.codeperf.agent.upload;

import com.cmb.codeperf.agent.session.SessionData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsyncDynamicEvidenceReporterTest {

    @Test
    public void should_ReturnImmediately_When_UploaderIsBlocked() throws Exception {
        BlockingUploader uploader = new BlockingUploader();
        AsyncDynamicEvidenceReporter reporter = new AsyncDynamicEvidenceReporter(uploader, 8);
        SessionData session = new SessionData();
        session.setEntryMethod("GET");
        session.setEntryPath("/demo");

        long startNanos = System.nanoTime();
        reporter.report(session);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(elapsedMs < 200, "report must not wait for HTTP upload");
        assertTrue(uploader.awaitUploadStarted(), "background uploader should receive payload");
        assertEquals(0, uploader.getUploadCount());

        uploader.release();
        assertTrue(uploader.awaitUploadCompleted(), "background uploader should finish after release");
        assertEquals(1, uploader.getUploadCount());
        reporter.close();
    }

    private static class BlockingUploader extends DynamicEvidenceUploader {

        private final CountDownLatch uploadStarted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch uploadCompleted = new CountDownLatch(1);
        private final AtomicInteger uploadCount = new AtomicInteger();

        private BlockingUploader() {
            super("http://127.0.0.1:1", "task-1");
        }

        @Override
        public void upload(String payload) throws IOException {
            uploadStarted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("upload interrupted", e);
            }
            uploadCount.incrementAndGet();
            uploadCompleted.countDown();
        }

        private boolean awaitUploadStarted() throws InterruptedException {
            return uploadStarted.await(2, TimeUnit.SECONDS);
        }

        private void release() {
            release.countDown();
        }

        private boolean awaitUploadCompleted() throws InterruptedException {
            return uploadCompleted.await(2, TimeUnit.SECONDS);
        }

        private int getUploadCount() {
            return uploadCount.get();
        }
    }
}
