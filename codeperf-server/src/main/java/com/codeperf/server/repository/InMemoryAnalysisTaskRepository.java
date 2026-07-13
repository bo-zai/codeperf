package com.codeperf.server.repository;

import com.codeperf.server.model.AnalysisTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "codeperf.storage.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAnalysisTaskRepository implements AnalysisTaskRepository {

    private final Map<String, AnalysisTask> tasks = new ConcurrentHashMap<>();

    @Override
    public AnalysisTask save(AnalysisTask task) {
        tasks.put(task.getAnalysisTaskId(), task);
        return task;
    }

    @Override
    public Optional<AnalysisTask> findByTaskId(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
