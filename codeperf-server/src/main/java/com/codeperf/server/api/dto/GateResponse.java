package com.codeperf.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GateResponse {
    private String analysisTaskId;
    private String status;
    private String riskLevel;
}
