package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.logging.AgentLogger;
import com.cmb.codeperf.agent.session.CallNode;
import com.cmb.codeperf.agent.session.IoEvent;
import com.cmb.codeperf.agent.session.RequestData;
import com.cmb.codeperf.agent.session.SessionData;
import com.cmb.codeperf.agent.session.SqlRecord;
import com.cmb.codeperf.agent.session.StackSample;
import com.cmb.codeperf.agent.upload.DynamicEvidenceReporter;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步发布采集会话，统一承载本地落盘和服务端上报。
 * 业务线程只做快照和入队，磁盘 I/O 与网络 I/O 固定在后台线程执行。
 */
public class AsyncSessionPublisher implements AutoCloseable {

    private static final int DEFAULT_QUEUE_SIZE = 1024;

    private final SessionWriter writer;
    private final DynamicEvidenceReporter reporter;
    private final BlockingQueue<SessionData> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong dropped = new AtomicLong();
    private final Thread worker;

    public AsyncSessionPublisher(SessionWriter writer, DynamicEvidenceReporter reporter) {
        this(writer, reporter, DEFAULT_QUEUE_SIZE);
    }

    public AsyncSessionPublisher(SessionWriter writer, DynamicEvidenceReporter reporter, int queueSize) {
        this.writer = writer;
        this.reporter = reporter;
        this.queue = new ArrayBlockingQueue<>(Math.max(queueSize, 1));
        this.worker = new Thread(this::runPublishLoop, "codeperf-session-publish");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /**
     * 发布一次会话快照。
     *
     * @param session 当前采集会话
     */
    public void publish(SessionData session) {
        try {
            SessionData snapshot = snapshot(session);
            if (queue.offer(snapshot)) {
                return;
            }
            long droppedCount = dropped.incrementAndGet();
            if (droppedCount == 1 || droppedCount % 100 == 0) {
                AgentLogger.error("session publish queue full, dropped=" + droppedCount);
            }
        } catch (Throwable t) {
            AgentLogger.error("session publish failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private SessionData snapshot(SessionData session) {
        synchronized (session) {
            return copySession(session);
        }
    }

    private SessionData copySession(SessionData source) {
        SessionData target = new SessionData();
        target.setEntryMethod(source.getEntryMethod());
        target.setEntryPath(source.getEntryPath());
        target.setTargetPackages(copyList(source.getTargetPackages()));
        target.setExcludedPackages(copyList(source.getExcludedPackages()));
        target.setStartTimeEpochMs(source.getStartTimeEpochMs());
        target.setJavaVersion(source.getJavaVersion());
        for (RequestData request : source.getRequests()) {
            target.addRequest(copyRequest(request));
        }
        return target;
    }

    private RequestData copyRequest(RequestData source) {
        RequestData target = new RequestData();
        target.setHttpMethod(source.getHttpMethod());
        target.setPath(source.getPath());
        target.setStatus(source.getStatus());
        target.setWallTimeMs(source.getWallTimeMs());
        target.setThreadName(source.getThreadName());
        target.setThreadId(source.getThreadId());
        target.setAllocBytes(source.getAllocBytes());
        target.setCallTree(copyCallNode(source.getCallTree()));
        for (SqlRecord sql : source.getSqls()) {
            SqlRecord copied = target.sqlRecord(sql.getFingerprint(), sql.getSampleSql());
            copied.setCount(sql.getCount());
            copied.setTotalMs(sql.getTotalMs());
            copied.setMaxMs(sql.getMaxMs());
            copied.setSlow(sql.isSlow());
        }
        for (StackSample sample : source.getSamples()) {
            target.addSample(new StackSample(copyList(sample.getFrames())));
        }
        for (IoEvent event : source.getIoEvents()) {
            target.addIoEvent(copyIoEvent(event));
        }
        return target;
    }

    private CallNode copyCallNode(CallNode source) {
        if (source == null) {
            return null;
        }
        CallNode target = new CallNode();
        target.setMethod(source.getMethod());
        target.setCount(source.getCount());
        target.setTotalTimeMs(source.getTotalTimeMs());
        target.setSelfTimeMs(source.getSelfTimeMs());
        for (CallNode child : source.getChildren()) {
            target.getChildren().add(copyCallNode(child));
        }
        return target;
    }

    private IoEvent copyIoEvent(IoEvent source) {
        IoEvent target = new IoEvent();
        target.setIoType(source.getIoType());
        target.setFramework(source.getFramework());
        target.setOperation(source.getOperation());
        target.setTarget(source.getTarget());
        target.setMappedStatementId(source.getMappedStatementId());
        target.setClassName(source.getClassName());
        target.setMethodName(source.getMethodName());
        target.setBusinessCallPath(source.getBusinessCallPath());
        target.setCount(source.getCount());
        target.setElapsedMs(source.getElapsedMs());
        return target;
    }

    private <T> ArrayList<T> copyList(java.util.List<T> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(source);
    }

    private void runPublishLoop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                SessionData payload = queue.poll(1, TimeUnit.SECONDS);
                if (payload == null) {
                    continue;
                }
                writeAndReport(payload);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running.get()) {
                    return;
                }
            } catch (Throwable t) {
                AgentLogger.error("session async publish loop failed: "
                        + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }

    private void writeAndReport(SessionData currentSession) {
        long startNanos = System.nanoTime();
        boolean localWritten = false;
        boolean uploaded = false;
        if (writer != null) {
            localWritten = writer.write(currentSession);
        }
        if (reporter != null) {
            try {
                reporter.report(currentSession);
                uploaded = true;
            } catch (Throwable t) {
                AgentLogger.error("dynamic evidence upload failed: "
                        + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        } else {
            AgentLogger.info("dynamic evidence upload skipped, uploadEnabled=false");
        }
        AgentLogger.info("session async publish completed, requests=" + requestCount(currentSession)
                + ", ioEvents=" + ioEventCount(currentSession)
                + ", localWritten=" + localWritten
                + ", uploaded=" + uploaded
                + ", queueRemaining=" + queue.size()
                + ", elapsedMs=" + elapsedMs(startNanos));
    }

    private int requestCount(SessionData session) {
        return session.getRequests().size();
    }

    private int ioEventCount(SessionData session) {
        int count = 0;
        for (RequestData request : session.getRequests()) {
            count += request.getIoEvents().size();
        }
        return count;
    }

    private long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    @Override
    public void close() {
        running.set(false);
        worker.interrupt();
    }
}
