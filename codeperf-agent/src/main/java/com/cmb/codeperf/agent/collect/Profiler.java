package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.session.RequestData;

/**
 * 可插拔的 CPU 采样器接口（可移植性决策③，见 docs/02-agent-core.md 第 5.3 节）。
 * MVP 实现：{@link JavaStackSampler}（纯 Java 线程栈采样）。
 * CI/Linux 期可加 AsyncProfilerCollector 实现而不改动其他代码。
 */
public interface Profiler {

    void start();

    void stop();

    /** 设定当前要采样的目标请求线程；采样所得栈帧写入对应 RequestData。 */
    void setTarget(Thread thread, RequestData request);

    void clearTarget();
}

