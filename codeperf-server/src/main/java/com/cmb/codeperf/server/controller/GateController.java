package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.model.dto.response.GateResponse;
import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.service.impl.AnalysisTaskService;
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
        AnalysisTaskBO task = service.get(taskId);
        return new GateResponse(
                task.getAnalysisTaskId(),
                task.getStatus().name(),
                task.getRiskLevel().name());
    }
}

