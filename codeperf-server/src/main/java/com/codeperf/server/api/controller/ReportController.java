package com.codeperf.server.api.controller;

import com.codeperf.server.api.dto.ReportResponse;
import com.codeperf.server.application.service.AnalysisTaskService;
import com.codeperf.server.application.service.StaticReportSummarizer;
import com.codeperf.server.domain.model.AnalysisTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/report")
public class ReportController {

    private final AnalysisTaskService service;
    private final StaticReportSummarizer staticReportSummarizer;

    public ReportController(AnalysisTaskService service,
                            StaticReportSummarizer staticReportSummarizer) {
        this.service = service;
        this.staticReportSummarizer = staticReportSummarizer;
    }

    @GetMapping
    public ReportResponse get(@PathVariable String taskId) {
        AnalysisTask task = service.get(taskId);
        return new ReportResponse(
                task.getAnalysisTaskId(),
                task.getProject(),
                task.getRemoteUrl(),
                task.getCommit(),
                task.getBranch(),
                task.getEnv(),
                task.getAuthorName(),
                task.getAuthorEmail(),
                task.getAuthorTime(),
                task.getCommitterName(),
                task.getCommitterEmail(),
                task.getCommitMessage(),
                task.getStatus().name(),
                task.getStaticRiskLevel().name(),
                task.getStaticPayload() != null,
                task.getDynamicPayload() != null,
                task.getRiskLevel().name(),
                staticReportSummarizer.summarize(task.getStaticPayload()));
    }
}
