package com.codeperf.server.controller;

import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.service.impl.AnalysisTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收不携带 taskId 的动态证据。
 * CI/CD 场景下 agent 配置长期固定，Server 通过提交身份把动态证据挂回静态扫描任务。
 */
@RestController
@RequestMapping("/api/dynamic-evidence")
public class DynamicEvidenceIdentityController {

    private final AnalysisTaskService service;

    public DynamicEvidenceIdentityController(AnalysisTaskService service) {
        this.service = service;
    }

    @PostMapping
    public AnalysisTaskBO upload(@RequestBody String payload) {
        return service.acceptDynamicEvidenceByIdentity(payload);
    }
}
