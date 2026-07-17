package com.codeperf.analysis.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 风险归因：表示风险的责任归属和变更范围。
 * <p>
 * 归因类型：
 * <ul>
 *   <li>NEW：本次新增代码引入的风险</li>
 *   <li>MODIFIED：本次修改代码引入的风险</li>
 *   <li>HISTORICAL：历史遗留风险（不在本次变更范围内）</li>
 *   <li>UNKNOWN：无法归因（全量扫描或缺少 git 信息）</li>
 * </ul>
 * <p>
 * 用途：门禁判定时仅阻断 NEW 和 MODIFIED 风险，HISTORICAL 风险仅报告。
 */
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
