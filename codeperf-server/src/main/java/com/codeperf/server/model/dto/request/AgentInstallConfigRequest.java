package com.codeperf.server.model.dto.request;

import lombok.Data;

@Data
public class AgentInstallConfigRequest {
    private String project;
    private String remoteUrl;
    private String commit;
    private String branch;
    private String env;
    private String authorName;
    private String authorEmail;
    private String commitTime;
    private String commitMessage;
}
