package com.codeperf.server.controller;

import com.codeperf.server.config.AgentInstallProperties;
import com.codeperf.server.model.dto.request.AgentInstallConfigRequest;
import com.codeperf.server.model.dto.response.AgentInstallConfigResponse;
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
@RestController
@RequestMapping("/api/agent/install-config")
public class AgentInstallConfigController {

    private final AgentInstallProperties properties;

    public AgentInstallConfigController(AgentInstallProperties properties) {
        this.properties = properties;
    }

    @PostMapping
    public AgentInstallConfigResponse config(@RequestBody AgentInstallConfigRequest request) {
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
