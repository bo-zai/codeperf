package com.codeperf.agent;

import com.codeperf.agent.collect.InstrumentationInstaller;
import com.codeperf.agent.collect.JavaStackSampler;
import com.codeperf.agent.collect.Profiler;
import com.codeperf.agent.collect.Recorder;
import com.codeperf.agent.collect.SessionWriter;
import com.codeperf.agent.config.AgentConfig;

import java.lang.instrument.Instrumentation;

/**
 * 装配采集核心：解析参数 → 建采样器/落盘器 → 初始化 Recorder → 启动采样 → 织入插桩。
 * premain 与 agentmain 都委托到此处，共用同一套核心（可移植性决策①）。
 * 见 docs/02-agent-core.md 第 2 节。
 */
public final class AgentBootstrap {

    private AgentBootstrap() {
    }

    public static synchronized void start(String args, Instrumentation inst) {
        try {
            AgentConfig cfg = AgentConfig.parse(args);
            System.out.println("[codeperf] agent starting, " + cfg);

            Profiler sampler = new JavaStackSampler(cfg.getSampleMs());
            SessionWriter writer = new SessionWriter(cfg.getOutput());
            Recorder.init(cfg, sampler, writer);
            sampler.start();

            new InstrumentationInstaller().install(cfg, inst);
            System.out.println("[codeperf] instrumentation installed; waiting for entry: "
                    + cfg.getEntryMethod() + " " + cfg.getEntryPath());
        } catch (Throwable t) {
            System.err.println("[codeperf] agent failed to start: " + t);
            t.printStackTrace();
        }
    }
}
