package com.codeperf.server.api;

import com.codeperf.server.api.dto.GateResponse;
import com.codeperf.server.model.AnalysisTask;
import com.codeperf.server.service.AnalysisTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/gate")
public class GateController {

    private final AnalysisTaskService service;

    public GateController(AnalysisTaskService service) {
        this.service = service;
    }

    @GetMapping
    public GateResponse get(@PathVariable String taskId) {
        AnalysisTask task = service.get(taskId);
        return new GateResponse(
                task.getAnalysisTaskId(),
                task.getStatus().name(),
                task.getRiskLevel().name());
    }
}
