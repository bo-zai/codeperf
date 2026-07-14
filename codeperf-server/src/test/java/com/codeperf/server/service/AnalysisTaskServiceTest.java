package com.codeperf.server.service;

import com.codeperf.server.model.AnalysisTask;
import com.codeperf.server.model.RiskLevel;
import com.codeperf.server.repository.InMemoryAnalysisTaskRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysisTaskServiceTest {

    @Test
    public void should_PersistTaskState_When_StaticAndDynamicEvidenceAccepted() {
        AnalysisTaskService service = new AnalysisTaskService(
                new InMemoryAnalysisTaskRepository(), new StaticReportSummarizer());

        AnalysisTask created = service.create("order-service", "abc", "main", "local");
        service.acceptStaticResult(created.getAnalysisTaskId(), "{\"findings\":[{\"severity\":\"WARN\"}]}");
        service.acceptDynamicEvidence(created.getAnalysisTaskId(), "{\"entry\":\"POST /api/orders/report\"}");

        AnalysisTask loaded = service.get(created.getAnalysisTaskId());
        assertEquals(RiskLevel.WARN, loaded.getRiskLevel());
        assertEquals(RiskLevel.WARN, loaded.getStaticRiskLevel());
        assertEquals("{\"entry\":\"POST /api/orders/report\"}", loaded.getDynamicPayload());
    }
}
