package com.cmb.codeperf.server.model.bo;

import lombok.Data;

/**
 * 问题出现记录。
 * 用于保留某个问题在哪一次分析任务中出现，后续可以据此判断修复、复现和通知历史。
 */
@Data
public class FindingOccurrenceBO {

    private Long issueId;
    private String taskId;
    private String occurrenceType;
    private String riskScope;
    private String severity;
    private String confidence;
    private String rawPayload;
}
