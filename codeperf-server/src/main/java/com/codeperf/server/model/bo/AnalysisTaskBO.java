package com.codeperf.server.model.bo;

import lombok.Data;

@Data
public class AnalysisTaskBO {
    private final String analysisTaskId;
    private final String project;
    private final String remoteUrl;
    private final String commit;
    private final String branch;
    private final String env;
    private final String authorName;
    private final String authorEmail;
    private final String authorTime;
    private final String committerName;
    private final String committerEmail;
    private final String commitMessage;
    private TaskStatus status;
    private RiskLevel riskLevel;
    private RiskLevel staticRiskLevel;
    private String staticPayload;
    private String dynamicPayload;

    public AnalysisTaskBO(String analysisTaskId, String project, String commit, String branch, String env) {
        this(analysisTaskId, project, "", commit, branch, env, "", "", "", "", "", "");
    }

    public AnalysisTaskBO(String analysisTaskId, String project, String remoteUrl, String commit, String branch, String env,
                        String authorName, String authorEmail, String authorTime,
                        String committerName, String committerEmail, String commitMessage) {
        this.analysisTaskId = analysisTaskId;
        this.project = project;
        this.remoteUrl = remoteUrl;
        this.commit = commit;
        this.branch = branch;
        this.env = env;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.authorTime = authorTime;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
        this.commitMessage = commitMessage;
        this.status = TaskStatus.CREATED;
        this.riskLevel = RiskLevel.NONE;
        this.staticRiskLevel = RiskLevel.NONE;
    }
}
