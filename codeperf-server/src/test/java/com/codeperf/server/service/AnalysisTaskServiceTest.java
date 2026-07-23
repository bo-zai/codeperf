package com.codeperf.server.service;

import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.model.bo.AnalysisTaskCreateBO;
import com.codeperf.server.model.bo.RiskLevel;
import com.codeperf.server.service.impl.AnalysisTaskService;
import com.codeperf.server.service.impl.StaticReportSummarizer;
import com.codeperf.server.service.repository.memory.InMemoryAnalysisTaskRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AnalysisTaskServiceTest {

    @Test
    public void should_PersistTaskState_When_StaticAndDynamicEvidenceAccepted() {
        AnalysisTaskService service = new AnalysisTaskService(
                new InMemoryAnalysisTaskRepository(), new StaticReportSummarizer());

        AnalysisTaskBO created = service.create("order-service", "abc", "main", "local");
        service.acceptStaticResult(created.getAnalysisTaskId(),
                "{\"findings\":[{\"ruleId\":\"LOOP_IO_AMPLIFICATION\",\"severity\":\"WARN\"}]}");
        service.acceptDynamicEvidence(created.getAnalysisTaskId(), "{\"entry\":\"POST /api/orders/report\"}");

        AnalysisTaskBO loaded = service.get(created.getAnalysisTaskId());
        assertEquals(RiskLevel.WARN, loaded.getRiskLevel());
        assertEquals(RiskLevel.WARN, loaded.getStaticRiskLevel());
        assertEquals("{\"entry\":\"POST /api/orders/report\"}", loaded.getDynamicPayload());
    }

    @Test
    public void should_LinkDynamicEvidenceToStaticTask_When_CommitIdentityMatches() {
        AnalysisTaskService service = new AnalysisTaskService(
                new InMemoryAnalysisTaskRepository(), new StaticReportSummarizer());

        AnalysisTaskCreateBO command = new AnalysisTaskCreateBO();
        command.setProject("order-service");
        command.setRemoteUrl("git@gitlab.company.com:mall/order-service.git");
        command.setCommit("abc");
        command.setBranch("main");
        command.setEnv("dev");
        command.setAuthorName("Alice Dev");
        command.setAuthorEmail("alice@example.com");
        command.setAuthorTime("2026-07-17T15:00:00+08:00");
        command.setCommitterName("Alice Dev");
        command.setCommitterEmail("alice@example.com");
        command.setCommitMessage("add order report");
        AnalysisTaskBO created = service.create(command);
        String payload = "{"
                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                + "\"commit\":\"abc\","
                + "\"branch\":\"main\","
                + "\"env\":\"dev\","
                + "\"appName\":\"order-service\","
                + "\"evidence\":{\"entryMethod\":\"POST\",\"entryPath\":\"/api/orders/report\"}"
                + "}";

        service.acceptDynamicEvidenceByIdentity(payload);

        AnalysisTaskBO loaded = service.get(created.getAnalysisTaskId());
        assertEquals(payload, loaded.getDynamicPayload());
    }

    @Test
    public void should_LinkDynamicEvidenceToLatestTask_When_SameCommitScannedMultipleTimes() {
        AnalysisTaskService service = new AnalysisTaskService(
                new InMemoryAnalysisTaskRepository(), new StaticReportSummarizer());

        AnalysisTaskCreateBO firstCommand = sameCommitCommand();
        AnalysisTaskBO first = service.create(firstCommand);
        AnalysisTaskBO latest = service.create(sameCommitCommand());
        String payload = "{"
                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                + "\"commit\":\"abc\","
                + "\"branch\":\"main\","
                + "\"env\":\"dev\","
                + "\"appName\":\"order-service\","
                + "\"evidence\":{\"entryMethod\":\"POST\",\"entryPath\":\"/api/orders/report\"}"
                + "}";

        service.acceptDynamicEvidenceByIdentity(payload);

        assertNull(service.get(first.getAnalysisTaskId()).getDynamicPayload());
        assertEquals(payload, service.get(latest.getAnalysisTaskId()).getDynamicPayload());
    }

    private AnalysisTaskCreateBO sameCommitCommand() {
        AnalysisTaskCreateBO command = new AnalysisTaskCreateBO();
        command.setProject("order-service");
        command.setRemoteUrl("git@gitlab.company.com:mall/order-service.git");
        command.setCommit("abc");
        command.setBranch("main");
        command.setEnv("dev");
        command.setAuthorName("Alice Dev");
        command.setAuthorEmail("alice@example.com");
        command.setAuthorTime("2026-07-17T15:00:00+08:00");
        command.setCommitterName("Alice Dev");
        command.setCommitterEmail("alice@example.com");
        command.setCommitMessage("add order report");
        return command;
    }
}
