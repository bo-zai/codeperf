package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.service.impl.AnalysisTaskService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/dynamic-evidence")
public class DynamicEvidenceController {

    private final AnalysisTaskService service;

    public DynamicEvidenceController(AnalysisTaskService service) {
        this.service = service;
    }

    @PostMapping
    public AnalysisTaskBO upload(@PathVariable String taskId, @RequestBody String payload) {
        return service.acceptDynamicEvidence(taskId, payload);
    }
}

