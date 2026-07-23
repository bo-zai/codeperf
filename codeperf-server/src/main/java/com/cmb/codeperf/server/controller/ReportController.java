package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.model.dto.response.ReportResponse;
import com.cmb.codeperf.server.service.impl.AnalysisTaskService;
import com.cmb.codeperf.server.service.impl.StaticReportSummarizer;
import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
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
        AnalysisTaskBO task = service.get(taskId);
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

