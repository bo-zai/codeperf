package com.codeperf.server.controller;

import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.service.impl.AnalysisTaskService;
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
    public AnalysisTaskBO upload(@PathVariable String taskId, @RequestBody String payload) {
        return service.acceptStaticResult(taskId, payload);
    }
}
