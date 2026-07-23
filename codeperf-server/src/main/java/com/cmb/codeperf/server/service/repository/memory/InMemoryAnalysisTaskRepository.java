package com.cmb.codeperf.server.service.repository.memory;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.StaticFindingBO;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "codeperf.storage.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAnalysisTaskRepository implements AnalysisTaskRepository {

    private final Map<String, AnalysisTaskBO> tasks = new ConcurrentHashMap<>();
    private final List<String> taskOrder = new CopyOnWriteArrayList<>();
    private final Map<String, List<StaticFindingBO>> staticFindings = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicEvidenceBO>> dynamicEvidence = new ConcurrentHashMap<>();
    private final Set<String> ruleIds = new HashSet<>(Arrays.asList("LOOP_IO_AMPLIFICATION"));

    @Override
    public AnalysisTaskBO save(AnalysisTaskBO task) {
        if (!tasks.containsKey(task.getAnalysisTaskId())) {
            taskOrder.add(task.getAnalysisTaskId());
        }
        tasks.put(task.getAnalysisTaskId(), task);
        return task;
    }

    @Override
    public Optional<AnalysisTaskBO> findByTaskId(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public Optional<AnalysisTaskBO> findLatestByCommitIdentity(String remoteUrl, String commit, String branch, String env) {
        for (int i = taskOrder.size() - 1; i >= 0; i--) {
            AnalysisTaskBO task = tasks.get(taskOrder.get(i));
            if (task == null) {
                continue;
            }
            if (same(task.getRemoteUrl(), remoteUrl)
                    && same(task.getCommit(), commit)
                    && same(task.getBranch(), branch)
                    && same(task.getEnv(), env)) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    @Override
    public void replaceStaticFindings(String taskId, List<StaticFindingBO> findings) {
        staticFindings.put(taskId, new ArrayList<>(findings));
    }

    @Override
    public void appendDynamicEvidence(DynamicEvidenceBO evidence) {
        dynamicEvidence.computeIfAbsent(evidence.getTaskId(), ignored -> new ArrayList<>()).add(evidence);
    }

    @Override
    public boolean isRuleDefined(String ruleId) {
        return ruleIds.contains(ruleId);
    }

    private boolean same(String left, String right) {
        return value(left).equals(value(right));
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}

