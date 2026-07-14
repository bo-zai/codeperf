package com.codeperf.server.api.controller;

import com.codeperf.server.domain.model.AnalysisTask;
import com.codeperf.server.application.service.AnalysisTaskService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/static-results")
public class StaticResultController {

    private final AnalysisTaskService service;

    public StaticResultController(AnalysisTaskService service) {
        this.service = service;
    }

    @PostMapping
    public AnalysisTask upload(@PathVariable String taskId, @RequestBody String payload) {
        return service.acceptStaticResult(taskId, payload);
    }
}
