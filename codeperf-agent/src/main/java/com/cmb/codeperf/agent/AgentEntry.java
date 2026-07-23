package com.cmb.codeperf.agent;

import java.lang.instrument.Instrumentation;

/**
 * Agent 启动入口。
 * 正式检测只支持 -javaagent 启动方式，不提供运行时 attach 入口。
 */
public final class AgentEntry {

    private AgentEntry() {
    }

    public static void premain(String args, Instrumentation inst) {
        AgentBootstrap.start(args, inst);
    }
}

