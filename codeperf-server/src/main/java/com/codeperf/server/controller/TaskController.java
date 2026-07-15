package com.codeperf.server.controller;

import com.codeperf.server.model.dto.request.CreateTaskRequest;
import com.codeperf.server.model.dto.response.CreateTaskResponse;
import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.model.bo.AnalysisTaskCreateBO;
import com.codeperf.server.service.impl.AnalysisTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final AnalysisTaskService service;

    public TaskController(AnalysisTaskService service) {
        this.service = service;
    }

    @PostMapping
    public CreateTaskResponse create(@RequestBody CreateTaskRequest request) {
        AnalysisTaskBO task = service.create(toCommand(request));
        return new CreateTaskResponse(task.getAnalysisTaskId());
    }

    @GetMapping("/{taskId}")
    public AnalysisTaskBO get(@PathVariable String taskId) {
        return service.get(taskId);
    }

    private AnalysisTaskCreateBO toCommand(CreateTaskRequest request) {
        AnalysisTaskCreateBO command = new AnalysisTaskCreateBO();
        command.setProject(request.getProject());
        command.setRemoteUrl(request.getRemoteUrl());
        command.setCommit(request.getCommit());
        command.setBranch(request.getBranch());
        command.setEnv(request.getEnv());
        command.setAuthorName(request.getAuthorName());
        command.setAuthorEmail(request.getAuthorEmail());
        command.setAuthorTime(request.getAuthorTime());
        command.setCommitterName(request.getCommitterName());
        command.setCommitterEmail(request.getCommitterEmail());
        command.setCommitMessage(request.getCommitMessage());
        return command;
    }
}
