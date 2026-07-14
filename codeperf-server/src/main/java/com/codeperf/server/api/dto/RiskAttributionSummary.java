package com.codeperf.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAttributionSummary {
    private String riskScope;
    private boolean changedLine;
    private String attributionConfidence;
    private String introducedByName;
    private String introducedByEmail;
    private String introducedCommit;
    private String introducedCommitTime;
    private String introducedCommitMessage;
}
