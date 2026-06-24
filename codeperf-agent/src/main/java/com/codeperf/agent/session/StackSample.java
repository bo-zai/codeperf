package com.codeperf.agent.session;

import java.util.List;

/**
 * 一次栈采样快照：某时刻目标请求线程的调用栈帧（栈顶在前）。
 * 分析端按帧聚合得到 CPU 热点。见 docs/02-agent-core.md 第 5.3 节。
 */
public class StackSample {

    private List<String> frames;

    public StackSample() {
    }

    public StackSample(List<String> frames) {
        this.frames = frames;
    }

    public List<String> getFrames() {
        return frames;
    }

    public void setFrames(List<String> frames) {
        this.frames = frames;
    }
}
