package com.codeperf.cli.cmd;

import com.codeperf.analysis.source.RiskAttribution;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.SourceScanResult;

public class StaticGateEvaluator {

    public StaticGateDecision evaluate(SourceScanResult result, String failOn, boolean attributionAware) {
        int blocking = 0;
        int newlyIntroduced = 0;
        int modified = 0;
        int historical = 0;
        int unknown = 0;

        for (SourceFinding finding : result.getFindings()) {
            if (!CommandSupport.shouldFail(finding.getSeverity().name(), failOn)) {
                continue;
            }
            if (!attributionAware) {
                blocking++;
                continue;
            }
            RiskAttribution.RiskScope riskScope = finding.getAttribution().getRiskScope();
            if (RiskAttribution.RiskScope.NEW.equals(riskScope)) {
                newlyIntroduced++;
                blocking++;
            } else if (RiskAttribution.RiskScope.MODIFIED.equals(riskScope)) {
                modified++;
                blocking++;
            } else if (RiskAttribution.RiskScope.HISTORICAL.equals(riskScope)) {
                historical++;
            } else {
                unknown++;
            }
        }
        return new StaticGateDecision(blocking > 0, blocking, newlyIntroduced, modified, historical, unknown);
    }
}
