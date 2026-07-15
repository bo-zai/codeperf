package com.codeperf.server.service;

import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.model.bo.RiskLevel;
import com.codeperf.server.service.impl.AnalysisTaskService;
import com.codeperf.server.service.impl.StaticReportSummarizer;
import com.codeperf.server.service.repository.memory.InMemoryAnalysisTaskRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysisTaskServiceTest {

    @Test
    public void should_PersistTaskState_When_StaticAndDynamicEvidenceAccepted() {
        AnalysisTaskService service = new AnalysisTaskService(
                new InMemoryAnalysisTaskRepository(), new StaticReportSummarizer());

        AnalysisTaskBO created = service.create("order-service", "abc", "main", "local");
        service.acceptStaticResult(created.getAnalysisTaskId(), "{\"findings\":[{\"severity\":\"WARN\"}]}");
        service.acceptDynamicEvidence(created.getAnalysisTaskId(), "{\"entry\":\"POST /api/orders/report\"}");

        AnalysisTaskBO loaded = service.get(created.getAnalysisTaskId());
        assertEquals(RiskLevel.WARN, loaded.getRiskLevel());
        assertEquals(RiskLevel.WARN, loaded.getStaticRiskLevel());
        assertEquals("{\"entry\":\"POST /api/orders/report\"}", loaded.getDynamicPayload());
    }
}
