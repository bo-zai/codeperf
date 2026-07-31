package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.config.AgentInstallProperties;
import com.cmb.codeperf.server.model.dto.request.AgentInstallConfigRequest;
import com.cmb.codeperf.server.model.dto.response.AgentInstallConfigResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 安装配置接口。
 * 安装脚本只调用这个稳定接口，具体 agent 版本、目标包名和采样参数由服务端统一下发。
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/install-config")
public class AgentInstallConfigController {

    private final AgentInstallProperties properties;

    public AgentInstallConfigController(AgentInstallProperties properties) {
        this.properties = properties;
    }

    @PostMapping
    public AgentInstallConfigResponse config(@RequestBody AgentInstallConfigRequest request) {
        log.info("event=codeperf.agent.install_config.request project={} remoteUrl={} commit={} branch={} env={} " +
                        "authorName={} authorEmail={} commitTime={} commitMessage={}",
                request.getProject(), request.getRemoteUrl(), request.getCommit(), request.getBranch(),
                request.getEnv(), request.getAuthorName(), request.getAuthorEmail(),
                request.getCommitTime(), request.getCommitMessage());
        AgentInstallConfigResponse response = new AgentInstallConfigResponse();
        response.setEnabled(properties.isEnabled());
        response.setServerUrl(properties.getServerUrl());
        response.setAgentUrl(properties.getAgentUrl());
        response.setAgentSha256(properties.getAgentSha256());
        response.setEnv(valueOrDefault(request.getEnv(), "dev"));
        response.setTargetPackages(splitPackages(properties.getTargetPackages()));
        response.setExcludedPackages(splitPackages(properties.getExcludedPackages()));
        response.setEntry(entry());
        response.setSlowSqlMs(properties.getSlowSqlMs());
        response.setSampleMs(properties.getSampleMs());
        response.setConnectTimeoutMs(properties.getConnectTimeoutMs());
        response.setReadTimeoutMs(properties.getReadTimeoutMs());
        response.setMode(properties.getMode());
        log.info("event=codeperf.agent.install_config.response enabled={} env={} targetPackages={} excludedPackages={} entryMethod={} entryPath={} mode={} connectTimeoutMs={} readTimeoutMs={}",
                response.isEnabled(), response.getEnv(), response.getTargetPackages(),
                response.getExcludedPackages(), response.getEntry().getMethod(), response.getEntry().getPath(),
                response.getMode(), response.getConnectTimeoutMs(), response.getReadTimeoutMs());
        return response;
    }

    private AgentInstallConfigResponse.AgentEntryConfig entry() {
        AgentInstallConfigResponse.AgentEntryConfig entry = new AgentInstallConfigResponse.AgentEntryConfig();
        entry.setMethod(valueOrDefault(properties.getEntryMethod(), AgentInstallProperties.DEFAULT_ENTRY_METHODS));
        entry.setPath(valueOrDefault(properties.getEntryPath(), "/"));
        return entry;
    }

    private List<String> splitPackages(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }
        for (String item : raw.split(",")) {
            String value = item.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
