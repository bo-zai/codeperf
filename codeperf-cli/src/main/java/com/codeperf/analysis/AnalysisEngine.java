package com.codeperf.analysis;

import com.codeperf.analysis.rules.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分析引擎：编排所有动态分析规则，汇总 Finding 并返回最高严重度。
 * <p>
 * 规则集：
 * <ul>
 *   <li>NPlusOneRule：检测循环内的数据库查询模式</li>
 *   <li>SlowSqlRule：检测执行时间超阈值的 SQL</li>
 *   <li>CpuHotspotRule：检测 CPU 占用高的方法</li>
 *   <li>HighAllocRule：检测内存分配热点</li>
 *   <li>HighLatencyRule：检测响应时间长的调用链</li>
 * </ul>
 * <p>
 * 见 docs/04-analysis-report.md 第 3 节。
 */
public class AnalysisEngine {

    private final List<AnalysisRule> rules = Arrays.asList(
            new NPlusOneRule(),
            new SlowSqlRule(),
            new CpuHotspotRule(),
            new HighAllocRule(),
            new HighLatencyRule()
    );

    /**
     * @return AnalysisResult 包含所有 findings 和最高严重度
     */
    public AnalysisResult run(JsonNode session) {
        List<Finding> allFindings = new ArrayList<>();
        for (AnalysisRule rule : rules) {
            allFindings.addAll(rule.analyze(session));
        }

        Severity maxSeverity = Severity.INFO;
        for (Finding f : allFindings) {
            if (f.getSeverity().getLevel() > maxSeverity.getLevel()) {
                maxSeverity = f.getSeverity();
            }
        }
        if (allFindings.isEmpty()) {
            maxSeverity = null; // 无问题
        }

        return new AnalysisResult(allFindings, maxSeverity);
    }

    public static class AnalysisResult {
        private final List<Finding> findings;
        private final Severity maxSeverity; // null 表示无问题

        public AnalysisResult(List<Finding> findings, Severity maxSeverity) {
            this.findings = findings;
            this.maxSeverity = maxSeverity;
        }

        public List<Finding> getFindings() {
            return findings;
        }

        public Severity getMaxSeverity() {
            return maxSeverity;
        }
    }
}
