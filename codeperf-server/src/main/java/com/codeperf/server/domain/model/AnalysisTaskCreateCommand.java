package com.codeperf.server.domain.model;

import lombok.Data;

@Data
public class AnalysisTaskCreateCommand {
    private String project;
    private String remoteUrl;
    private String commit;
    private String branch;
    private String env;
    private String authorName;
    private String authorEmail;
    private String authorTime;
    private String committerName;
    private String committerEmail;
    private String commitMessage;
}
