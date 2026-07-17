package com.codeperf.agent;

import com.codeperf.agent.collect.InstrumentationInstaller;
import com.codeperf.agent.collect.JavaStackSampler;
import com.codeperf.agent.collect.Profiler;
import com.codeperf.agent.collect.Recorder;
import com.codeperf.agent.collect.SessionWriter;
import com.codeperf.agent.config.AgentConfig;
import com.codeperf.agent.upload.DynamicEvidenceReporter;
import com.codeperf.agent.upload.DynamicEvidenceUploader;

import java.lang.instrument.Instrumentation;

/**
 * 装配采集核心：解析参数 → 建采样器/落盘器 → 初始化 Recorder → 启动采样 → 织入插桩。
 * 正式环境通过 -javaagent 在应用启动时加载，不支持运行时 attach。
 * 见 docs/02-agent-core.md 第 2 节。
 */
public final class AgentBootstrap {

    private AgentBootstrap() {
    }

    public static synchronized void start(String args, Instrumentation inst) {
        try {
            AgentConfig cfg = AgentConfig.load(args);
            System.out.println("[codeperf] agent starting, " + cfg);

            Profiler sampler = new JavaStackSampler(cfg.getSampleMs());
            SessionWriter writer = new SessionWriter(cfg.getOutput());
            DynamicEvidenceReporter reporter = createReporter(cfg);
            Recorder.init(cfg, sampler, writer, reporter);
            sampler.start();

            new InstrumentationInstaller().install(cfg, inst);
            System.out.println("[codeperf] instrumentation installed; waiting for entry: "
                    + cfg.getEntryMethod() + " " + cfg.getEntryPath());
        } catch (Throwable t) {
            System.err.println("[codeperf] agent failed to start: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static DynamicEvidenceReporter createReporter(AgentConfig cfg) {
        if (!cfg.isUploadEnabled()) {
            return null;
        }
        if (isBlank(cfg.getServerUrl())) {
            throw new IllegalArgumentException("uploadEnabled=true requires serverUrl");
        }
        validateUploadIdentity(cfg);
        return new DynamicEvidenceReporter(new DynamicEvidenceUploader(
                cfg.getServerUrl(), cfg.getAnalysisTaskId(), cfg.getAppName(), cfg.getEnv(),
                cfg.getRemoteUrl(), cfg.getCommit(), cfg.getBranch()));
    }

    private static void validateUploadIdentity(AgentConfig cfg) {
        if (!isBlank(cfg.getAnalysisTaskId())) {
            return;
        }
        if (isBlank(cfg.getRemoteUrl()) || isBlank(cfg.getCommit()) || isBlank(cfg.getBranch()) || isBlank(cfg.getEnv())) {
            throw new IllegalArgumentException(
                    "uploadEnabled=true requires analysisTaskId or build-info remoteUrl/commit/branch/env");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
