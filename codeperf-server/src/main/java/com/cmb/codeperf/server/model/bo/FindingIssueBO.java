package com.cmb.codeperf.server.model.bo;

import lombok.Data;

/**
 * 静态风险问题档案。
 * 该对象跨扫描任务存在，用于把多次 push 中发现的同一类风险沉淀为可跟踪的问题。
 */
@Data
public class FindingIssueBO {

    private Long id;
    private String issueKey;
    private Long repositoryId;
    private String branchName;
    private String ruleId;
    private String evidenceHash;
    private String sourceFile;
    private int lineNumber;
    private String loopMethodName;
    private String ioType;
    private String ownerName;
    private String ownerEmail;
    private String status;
    private String severity;
    private String firstSeenTaskId;
    private String lastSeenTaskId;
    private String rawPayload;
}
