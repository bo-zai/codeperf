package com.cmb.codeperf.server.service;

import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.dto.response.StaticFindingSummary;
import com.cmb.codeperf.server.model.vo.report.RuntimeCorroborationSummaryVO;
import com.cmb.codeperf.server.service.impl.RuntimeEvidenceAggregator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态证据聚合测试。
 * 聚合逻辑直接影响报告优先级，必须避免只取最大单条证据导致入口分布和命中频次丢失。
 */
public class RuntimeEvidenceAggregatorTest {

    private final RuntimeEvidenceAggregator aggregator = new RuntimeEvidenceAggregator();

    @Test
    public void should_AggregateHitsByStaticFinding_When_SameRiskHitMultipleTimes() {
        StaticFindingSummary finding = finding("orderMapper.selectById(userId)");
        List<DynamicEvidenceBO> records = Arrays.asList(
                evidence("GET /demo/pipeline/orders/reconciliation", 5),
                evidence("GET /demo/pipeline/orders/reconciliation", 3),
                evidence("GET /demo/export/rows", 20));

        Map<String, RuntimeCorroborationSummaryVO> summaries = aggregator.aggregate(Arrays.asList(finding), records);
        RuntimeCorroborationSummaryVO summary = summaries.get(aggregator.findingKey(finding));

        assertEquals("HIGH_AMPLIFICATION", summary.getStatus());
        assertEquals(3, summary.getHitRequestCount());
        assertEquals(2, summary.getHitEntryCount());
        assertEquals(20, summary.getMaxRepeatCount());
        assertEquals(9, summary.getAvgRepeatCount());
        assertEquals("GET /demo/export/rows", summary.getTopEntryKey());
        assertEquals("GET /demo/export/rows", summary.getLatestEntryKey());
        assertEquals(2, summary.getTopEntries().size());
        assertTrue(summary.getText().contains("命中请求 3 次"));
    }

    @Test
    public void should_ReturnNotHitSummary_When_NoRuntimeEvidenceMatches() {
        StaticFindingSummary finding = finding("orderMapper.selectById(userId)");

        Map<String, RuntimeCorroborationSummaryVO> summaries = aggregator.aggregate(
                Arrays.asList(finding),
                Arrays.asList(evidence("GET /demo/orders/preview", 2, "queryProfile")));

        RuntimeCorroborationSummaryVO summary = summaries.get(aggregator.findingKey(finding));

        assertEquals("NOT_HIT", summary.getStatus());
        assertEquals(0, summary.getHitRequestCount());
        assertEquals("当前没有找到能够直接对应这条静态风险的运行证据。", summary.getText());
    }

    private StaticFindingSummary finding(String evidence) {
        StaticFindingSummary finding = new StaticFindingSummary();
        finding.setRuleId("LOOP_IO_AMPLIFICATION");
        finding.setSourceFile("codeperf-demo-app/src/main/java/Demo.java");
        finding.setLineNumber(43);
        finding.setLoopStartLine(40);
        finding.setLoopEndLine(48);
        finding.setLoopMethodName("buildReconciliationRows");
        finding.setEvidence(evidence);
        finding.setIoType("DB");
        return finding;
    }

    private DynamicEvidenceBO evidence(String entryKey, int count) {
        return evidence(entryKey, count, "selectById");
    }

    private DynamicEvidenceBO evidence(String entryKey, int count, String methodName) {
        DynamicEvidenceBO evidence = new DynamicEvidenceBO();
        evidence.setTaskId("task-1");
        evidence.setAppName("codeperf-demo-app");
        evidence.setEnv("local");
        evidence.setEntryKey(entryKey);
        evidence.setRawPayload("{"
                + "\"evidence\":{\"requests\":[{"
                + "\"httpMethod\":\"" + entryKey.substring(0, entryKey.indexOf(' ')) + "\","
                + "\"path\":\"" + entryKey.substring(entryKey.indexOf(' ') + 1) + "\","
                + "\"callTree\":{\"method\":\"ROOT\",\"count\":0,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.controller.PipelineVerificationController.reconciliation\","
                + "\"count\":1,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.service.PipelineVerificationService.buildReconciliationRows\","
                + "\"count\":1,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.infrastructure.DemoOrderMapper." + methodName + "\","
                + "\"count\":" + count + ",\"children\":[]"
                + "}]"
                + "}]"
                + "}]}}"
                + "]}}");
        return evidence;
    }
}
