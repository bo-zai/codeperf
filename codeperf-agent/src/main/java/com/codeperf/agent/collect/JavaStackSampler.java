package com.codeperf.agent.collect;

import com.codeperf.agent.session.RequestData;
import com.codeperf.agent.session.StackSample;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯 Java 线程栈采样器（MVP 默认实现）。守护线程按固定周期对「目标请求线程」
 * 抓取调用栈，仅在线程处于 RUNNABLE 状态时记录（近似 CPU 占用）。
 * 见 docs/02-agent-core.md 第 5.3 节。
 */
public class JavaStackSampler implements Profiler {

    private final long sampleMs;
    private volatile Thread targetThread;
    private volatile RequestData targetRequest;
    private volatile boolean running;
    private Thread worker;

    public JavaStackSampler(long sampleMs) {
        this.sampleMs = sampleMs <= 0 ? 10 : sampleMs;
    }

    @Override
    public void start() {
        running = true;
        worker = new Thread(this::loop, "codeperf-sampler");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    @Override
    public void setTarget(Thread thread, RequestData request) {
        this.targetRequest = request;
        this.targetThread = thread;
    }

    @Override
    public void clearTarget() {
        this.targetThread = null;
        this.targetRequest = null;
    }

    private void loop() {
        while (running) {
            try {
                Thread t = targetThread;
                RequestData rd = targetRequest;
                if (t != null && rd != null && t.getState() == Thread.State.RUNNABLE) {
                    StackTraceElement[] trace = t.getStackTrace();
                    if (trace != null && trace.length > 0) {
                        List<String> frames = new ArrayList<>(trace.length);
                        for (StackTraceElement e : trace) {
                            frames.add(e.getClassName() + "." + e.getMethodName());
                        }
                        rd.addSample(new StackSample(frames));
                    }
                }
                Thread.sleep(sampleMs);
            } catch (InterruptedException ie) {
                if (!running) {
                    return;
                }
            } catch (Throwable ignore) {
                // 采样异常绝不影响业务
            }
        }
    }
}
