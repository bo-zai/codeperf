package com.cmb.codeperf.agent.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一次栈采样快照：某时刻目标请求线程的调用栈帧（栈顶在前）。
 * 分析端按帧聚合得到 CPU 热点。见 docs/02-agent-core.md 第 5.3 节。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StackSample {

    private List<String> frames;
}

