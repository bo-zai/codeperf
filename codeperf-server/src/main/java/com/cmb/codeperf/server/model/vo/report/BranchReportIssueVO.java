package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

/**
 * 分支报告中的单个开放风险。
 */
@Data
public class BranchReportIssueVO {

    private String sourceFile;
    private String fileName;
    private int lineNumber;
    private String ruleId;
    private String severity;
    private String loopMethodName;
    private String ioType;
    private String runtimeStatus;
    private String runtimeEntryKey;
    private String runtimeCallPath;
    private int runtimeRepeatCount;
    private String rawPayload;
}
