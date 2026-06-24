package com.codeperf.agent;

import java.lang.instrument.Instrumentation;

/**
 * Agent 双入口（见 docs/02-agent-core.md 第 2 节）：
 *  - premain：-javaagent 静态挂载（CI/CD 终态）；
 *  - agentmain：运行时 attach 动态挂载（本地 MVP）。
 * 二者共用 {@link AgentBootstrap#start}。
 */
public final class AgentEntry {

    private AgentEntry() {
    }

    public static void premain(String args, Instrumentation inst) {
        AgentBootstrap.start(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        AgentBootstrap.start(args, inst);
    }
}
