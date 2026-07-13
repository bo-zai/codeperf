package com.codeperf.server.model;

import lombok.Data;

@Data
public class AnalysisTask {
    private final String analysisTaskId;
    private final String project;
    private final String commit;
    private final String branch;
    private final String env;
    private TaskStatus status;
    private RiskLevel riskLevel;
    private RiskLevel staticRiskLevel;
    private String staticPayload;
    private String dynamicPayload;

    public AnalysisTask(String analysisTaskId, String project, String commit, String branch, String env) {
        this.analysisTaskId = analysisTaskId;
        this.project = project;
        this.commit = commit;
        this.branch = branch;
        this.env = env;
        this.status = TaskStatus.CREATED;
        this.riskLevel = RiskLevel.NONE;
        this.staticRiskLevel = RiskLevel.NONE;
    }
}
