package com.codeperf.analysis.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RiskAttribution {
    public enum RiskScope { NEW, MODIFIED, HISTORICAL, UNKNOWN }

    public enum AttributionConfidence { HIGH, MEDIUM, LOW }

    private final RiskScope riskScope;
    private final boolean changedLine;
    private final AttributionConfidence attributionConfidence;
    private final String introducedByName;
    private final String introducedByEmail;
    private final String introducedCommit;
    private final String introducedCommitTime;
    private final String introducedCommitMessage;

    public static RiskAttribution unknown() {
        return new RiskAttribution(
                RiskScope.UNKNOWN,
                false,
                AttributionConfidence.LOW,
                "",
                "",
                "",
                "",
                "");
    }
}
