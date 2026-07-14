package com.codeperf.server.api.controller;

import com.codeperf.server.api.dto.CreateTaskRequest;
import com.codeperf.server.api.dto.CreateTaskResponse;
import com.codeperf.server.domain.model.AnalysisTask;
import com.codeperf.server.domain.model.AnalysisTaskCreateCommand;
import com.codeperf.server.application.service.AnalysisTaskService;
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
        AnalysisTask task = service.create(toCommand(request));
        return new CreateTaskResponse(task.getAnalysisTaskId());
    }

    @GetMapping("/{taskId}")
    public AnalysisTask get(@PathVariable String taskId) {
        return service.get(taskId);
    }

    private AnalysisTaskCreateCommand toCommand(CreateTaskRequest request) {
        AnalysisTaskCreateCommand command = new AnalysisTaskCreateCommand();
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
