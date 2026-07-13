package com.codeperf.server.api;

import com.codeperf.server.api.dto.CreateTaskRequest;
import com.codeperf.server.api.dto.CreateTaskResponse;
import com.codeperf.server.model.AnalysisTask;
import com.codeperf.server.service.AnalysisTaskService;
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
        AnalysisTask task = service.create(
                request.getProject(),
                request.getCommit(),
                request.getBranch(),
                request.getEnv());
        return new CreateTaskResponse(task.getAnalysisTaskId());
    }

    @GetMapping("/{taskId}")
    public AnalysisTask get(@PathVariable String taskId) {
        return service.get(taskId);
    }
}
