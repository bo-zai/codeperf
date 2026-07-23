package com.cmb.codeperf.cli.cmd;

import com.cmb.codeperf.analysis.source.RiskAttribution;
import com.cmb.codeperf.analysis.source.SourceFinding;
import com.cmb.codeperf.analysis.source.SourceScanResult;

/**
 * 门禁评估器：根据风险严重度和归因判定是否阻断构建。
 * <p>
 * 判定规则：
 * <ul>
 *   <li>严重度过滤：仅当风险严重度达到或超过配置阈值时参与判定</li>
 *   <li>归因感知：开启时，仅 NEW 和 MODIFIED 风险计入阻断；HISTORICAL 不阻断</li>
 *   <li>非归因模式：所有达到阈值的风险均计入阻断（用于全量扫描场景）</li>
 * </ul>
 */
public class StaticGateEvaluator {

    /**
     * 评估扫描结果，生成门禁决策。
     *
     * @param result           扫描结果
     * @param failOn           阈值配置：NONE/INFO/WARN/CRITICAL
     * @param attributionAware 是否开启归因感知（仅新增风险阻断）
     * @return 门禁决策，包含阻断数、新增数、历史数等统计
     */
    public StaticGateDecision evaluate(SourceScanResult result, String failOn, boolean attributionAware) {
        int blocking = 0;
        int newlyIntroduced = 0;
        int modified = 0;
        int historical = 0;
        int unknown = 0;

        for (SourceFinding finding : result.getFindings()) {
            // 严重度过滤：低于阈值的风险不参与门禁判定
            if (!CommandSupport.shouldFail(finding.getSeverity().name(), failOn)) {
                continue;
            }
            // 非归因模式：所有达到阈值的风险均计入阻断（全量扫描场景）
            if (!attributionAware) {
                blocking++;
                continue;
            }
            // 归因感知模式：仅 NEW/MODIFIED 计入阻断，HISTORICAL 仅统计不阻断
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

