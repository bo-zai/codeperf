package com.cmb.codeperf.agent.upload;

import com.cmb.codeperf.agent.logging.AgentLogger;
import com.cmb.codeperf.agent.session.SessionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步动态证据报告器。
 * 业务线程只负责序列化快照并尝试入队，HTTP 上传固定由后台 daemon 线程执行。
 */
public class AsyncDynamicEvidenceReporter extends DynamicEvidenceReporter implements AutoCloseable {

    private static final int DEFAULT_QUEUE_SIZE = 1024;

    private final DynamicEvidenceUploader uploader;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<String> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong dropped = new AtomicLong();
    private final Thread worker;

    public AsyncDynamicEvidenceReporter(DynamicEvidenceUploader uploader) {
        this(uploader, DEFAULT_QUEUE_SIZE);
    }

    public AsyncDynamicEvidenceReporter(DynamicEvidenceUploader uploader, int queueSize) {
        super(null);
        if (uploader == null) {
            throw new IllegalArgumentException("uploader must not be null");
        }
        this.uploader = uploader;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.queue = new ArrayBlockingQueue<>(Math.max(queueSize, 1));
        this.worker = new Thread(this::runUploadLoop, "codeperf-dynamic-upload");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /**
     * 提交动态证据到后台队列。
     * 队列满时直接丢弃，避免 CodePerf 上报反压到真实业务请求。
     *
     * @param session 动态采集会话
     * @throws IOException 序列化失败时抛出
     */
    @Override
    public void report(SessionData session) throws IOException {
        String payload = objectMapper.writeValueAsString(session);
        if (queue.offer(payload)) {
            return;
        }
        long droppedCount = dropped.incrementAndGet();
        if (droppedCount == 1 || droppedCount % 100 == 0) {
            AgentLogger.error("dynamic evidence upload queue full, dropped=" + droppedCount);
        }
    }

    private void runUploadLoop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                String payload = queue.poll(1, TimeUnit.SECONDS);
                if (payload == null) {
                    continue;
                }
                upload(payload);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running.get()) {
                    return;
                }
            } catch (Throwable t) {
                AgentLogger.error("dynamic evidence async upload loop failed: "
                        + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }

    private void upload(String payload) {
        try {
            uploader.upload(payload);
            AgentLogger.info("dynamic evidence async upload succeeded");
        } catch (Throwable t) {
            AgentLogger.error("dynamic evidence async upload failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    @Override
    public void close() {
        running.set(false);
        worker.interrupt();
    }
}
