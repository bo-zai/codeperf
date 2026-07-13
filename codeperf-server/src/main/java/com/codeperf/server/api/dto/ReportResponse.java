package com.codeperf.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析报告摘要。
 * 明确区分静态结构风险、动态运行证据和最终门禁风险，避免把预发证据误称为生产实测。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String analysisTaskId;
    private String status;
    private String staticRiskLevel;
    private boolean hasStaticResult;
    private boolean hasDynamicEvidence;
    private String riskLevel;
}
