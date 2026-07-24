package com.cmb.codeperf.server.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AgentInstallConfigResponse {
    private boolean enabled;
    private String serverUrl;
    private String agentUrl;
    private String agentSha256;
    private String appName;
    private String env;
    private List<String> targetPackages;
    private List<String> excludedPackages;
    private AgentEntryConfig entry;
    private long slowSqlMs;
    private long sampleMs;
    private String mode;

    @Data
    public static class AgentEntryConfig {
        private String method;
        private String path;
    }
}

