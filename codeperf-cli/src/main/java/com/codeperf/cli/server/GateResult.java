package com.codeperf.cli.server;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端门禁响应。
 * CLI 根据 riskLevel 决定 Git/CI 流程是否失败。
 */
@Data
@NoArgsConstructor
public class GateResult {
    private String analysisTaskId;
    private String status;
    private String riskLevel;
}
