package com.codeperf.server.api;

import com.codeperf.server.api.dto.ReportResponse;
import com.codeperf.server.model.AnalysisTask;
import com.codeperf.server.service.AnalysisTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/report")
public class ReportController {

    private final AnalysisTaskService service;

    public ReportController(AnalysisTaskService service) {
        this.service = service;
    }

    @GetMapping
    public ReportResponse get(@PathVariable String taskId) {
        AnalysisTask task = service.get(taskId);
        return new ReportResponse(
                task.getAnalysisTaskId(),
                task.getStatus().name(),
                task.getStaticRiskLevel().name(),
                task.getStaticPayload() != null,
                task.getDynamicPayload() != null,
                task.getRiskLevel().name());
    }
}
