package com.codeperf.server.infrastructure.persistence.memory;

import com.codeperf.server.domain.model.AnalysisTask;
import com.codeperf.server.domain.model.DynamicEvidenceRecord;
import com.codeperf.server.domain.model.StaticFindingRecord;
import com.codeperf.server.domain.repository.AnalysisTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "codeperf.storage.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAnalysisTaskRepository implements AnalysisTaskRepository {

    private final Map<String, AnalysisTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, List<StaticFindingRecord>> staticFindings = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicEvidenceRecord>> dynamicEvidence = new ConcurrentHashMap<>();

    @Override
    public AnalysisTask save(AnalysisTask task) {
        tasks.put(task.getAnalysisTaskId(), task);
        return task;
    }

    @Override
    public Optional<AnalysisTask> findByTaskId(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public void replaceStaticFindings(String taskId, List<StaticFindingRecord> findings) {
        staticFindings.put(taskId, new ArrayList<>(findings));
    }

    @Override
    public void appendDynamicEvidence(DynamicEvidenceRecord evidence) {
        dynamicEvidence.computeIfAbsent(evidence.getTaskId(), ignored -> new ArrayList<>()).add(evidence);
    }
}
