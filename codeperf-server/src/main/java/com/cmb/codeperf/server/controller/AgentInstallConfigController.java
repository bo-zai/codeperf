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
        log.info("event=codeperf.agent.install_config.request project={} remoteUrl={} commit={} branch={} env={} authorName={} authorEmail={} commitTime={} commitMessage={}",
                request.getProject(), request.getRemoteUrl(), request.getCommit(), request.getBranch(), request.getEnv(),
                request.getAuthorName(), request.getAuthorEmail(), request.getCommitTime(), request.getCommitMessage());
        AgentInstallConfigResponse response = new AgentInstallConfigResponse();
        response.setEnabled(properties.isEnabled());
        response.setServerUrl(properties.getServerUrl());
        response.setAgentUrl(properties.getAgentUrl());
        response.setAgentSha256(properties.getAgentSha256());
        response.setAppName(valueOrDefault(request.getProject(), repoName(request.getRemoteUrl())));
        response.setEnv(valueOrDefault(request.getEnv(), "dev"));
        response.setTargetPackages(targetPackages());
        response.setEntry(entry());
        response.setSlowSqlMs(properties.getSlowSqlMs());
        response.setSampleMs(properties.getSampleMs());
        response.setMode(properties.getMode());
        log.info("event=codeperf.agent.install_config.response enabled={} appName={} env={} targetPackages={} entryMethod={} entryPath={} mode={}",
                response.isEnabled(), response.getAppName(), response.getEnv(), response.getTargetPackages(),
                response.getEntry().getMethod(), response.getEntry().getPath(), response.getMode());
        return response;
    }

    private AgentInstallConfigResponse.AgentEntryConfig entry() {
        AgentInstallConfigResponse.AgentEntryConfig entry = new AgentInstallConfigResponse.AgentEntryConfig();
        entry.setMethod(valueOrDefault(properties.getEntryMethod(), "POST"));
        entry.setPath(valueOrDefault(properties.getEntryPath(), "/"));
        return entry;
    }

    private List<String> targetPackages() {
        List<String> values = new ArrayList<>();
        String raw = properties.getTargetPackages();
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

    private String repoName(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            return "";
        }
        String normalized = remoteUrl.trim().replace(':', '/').replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}

