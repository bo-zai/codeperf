package com.codeperf.server.model.bo;

import lombok.Data;

@Data
public class StaticFindingBO {
    private String taskId;
    private String ruleId;
    private String severity;
    private String confidence;
    private String sourceFile;
    private int lineNumber;
    private int loopStartLine;
    private int loopEndLine;
    private String loopMethodName;
    private String ioType;
    private String riskScope;
    private boolean changedLine;
    private String introducedByName;
    private String introducedByEmail;
    private String introducedCommit;
    private String introducedCommitTime;
    private String evidenceHash;
    private String rawPayload;
}
