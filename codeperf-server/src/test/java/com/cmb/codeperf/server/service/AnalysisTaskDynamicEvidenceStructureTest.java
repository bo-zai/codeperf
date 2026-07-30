package com.cmb.codeperf.server.service;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.DynamicCallEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicRequestEvidenceBO;
import com.cmb.codeperf.server.model.bo.StaticDynamicCorroborationBO;
import com.cmb.codeperf.server.service.impl.AnalysisTaskService;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 动态证据结构化落库测试。
 * 企业报告不能长期依赖页面解析原始JSON，动态上报时必须同步沉淀请求级和调用级证据。
 */
@SpringBootTest(properties = {
        "codeperf.storage.mode=memory",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
public class AnalysisTaskDynamicEvidenceStructureTest {

    @Autowired
    private AnalysisTaskService analysisTaskService;

    @Autowired
    private AnalysisTaskRepository repository;

    @Test
    public void should_SaveStructuredRequestAndCallEvidence_When_DynamicEvidenceAccepted() {
        AnalysisTaskBO task = analysisTaskService.create("order-service", "abc123", "main", "local");

        analysisTaskService.acceptDynamicEvidence(task.getAnalysisTaskId(), "{"
                + "\"evidence\":{"
                + "\"entryMethod\":\"GET\","
                + "\"entryPath\":\"/demo/pipeline/orders/reconciliation\","
                + "\"requests\":[{"
                + "\"httpMethod\":\"GET\","
                + "\"path\":\"/demo/pipeline/orders/reconciliation\","
                + "\"wallTimeMs\":42,"
                + "\"callTree\":{\"method\":\"ROOT\",\"count\":0,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.controller.PipelineVerificationController.reconciliation\","
                + "\"count\":1,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.service.PipelineVerificationService.buildReconciliationRows\","
                + "\"count\":1,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.infrastructure.DemoOrderMapper.selectById\","
                + "\"count\":5,\"totalMs\":11,\"children\":[]"
                + "}]"
                + "}]"
                + "}]}}"
                + "]}}");

        List<DynamicRequestEvidenceBO> requests = repository.listDynamicRequestEvidence(task.getAnalysisTaskId());
        List<DynamicCallEvidenceBO> calls = repository.listDynamicCallEvidence(task.getAnalysisTaskId());

        assertEquals(1, requests.size());
        assertEquals("GET /demo/pipeline/orders/reconciliation", requests.get(0).getEntryKey());
        assertFalse(calls.isEmpty());
        assertEquals("selectById", calls.get(calls.size() - 1).getMethodName());
        assertEquals(5, calls.get(calls.size() - 1).getCallCount());
    }

    @Test
    public void should_SaveStaticDynamicCorroboration_When_DynamicEvidenceHitsStaticFinding() {
        AnalysisTaskBO task = analysisTaskService.create("order-service", "abc123", "main", "local");
        analysisTaskService.acceptStaticResult(task.getAnalysisTaskId(), "{"
                + "\"filesScanned\":1,"
                + "\"findings\":[{"
                + "\"ruleId\":\"LOOP_IO_AMPLIFICATION\","
                + "\"severity\":\"WARN\","
                + "\"confidence\":\"HIGH\","
                + "\"sourceFile\":\"codeperf-demo-app/src/main/java/com/cmb/codeperf/demo/app/service/PipelineVerificationService.java\","
                + "\"evidence\":\"数据库访问调用: demoOrderMapper.selectById(orderId)\","
                + "\"lineNumber\":41,"
                + "\"loopStartLine\":40,"
                + "\"loopEndLine\":44,"
                + "\"ioType\":\"DB\","
                + "\"loopMethodName\":\"buildReconciliationRows\","
                + "\"loopCallLine\":41,"
                + "\"ioLine\":41,"
                + "\"attribution\":{\"riskScope\":\"NEW\"}"
                + "}],"
                + "\"parseErrors\":[]"
                + "}");

        analysisTaskService.acceptDynamicEvidence(task.getAnalysisTaskId(), "{"
                + "\"evidence\":{"
                + "\"entryMethod\":\"GET\","
                + "\"entryPath\":\"/demo/pipeline/orders/reconciliation\","
                + "\"requests\":[{"
                + "\"httpMethod\":\"GET\","
                + "\"path\":\"/demo/pipeline/orders/reconciliation\","
                + "\"callTree\":{\"method\":\"ROOT\",\"count\":0,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.controller.PipelineVerificationController.reconciliation\","
                + "\"count\":1,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.service.PipelineVerificationService.buildReconciliationRows\","
                + "\"count\":1,\"children\":[{"
                + "\"method\":\"com.cmb.codeperf.demo.app.infrastructure.DemoOrderMapper.selectById\","
                + "\"count\":5,\"children\":[]"
                + "}]"
                + "}]"
                + "}]}}"
                + "]}}");

        List<StaticDynamicCorroborationBO> corroborations =
                repository.listStaticDynamicCorroborations(task.getAnalysisTaskId());

        assertEquals(1, corroborations.size());
        assertEquals("HIT", corroborations.get(0).getStatus());
        assertNotNull(corroborations.get(0).getStaticFindingId());
        assertNotNull(corroborations.get(0).getFindingIssueId());
        assertEquals(1, corroborations.get(0).getHitRequestCount());
        assertEquals(5, corroborations.get(0).getMaxRepeatCount());
        assertEquals("GET /demo/pipeline/orders/reconciliation", corroborations.get(0).getTopEntryKey());
    }
}
