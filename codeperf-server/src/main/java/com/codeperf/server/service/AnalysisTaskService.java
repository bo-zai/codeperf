package com.codeperf.server.service;

import com.codeperf.server.model.AnalysisTask;
import com.codeperf.server.model.RiskLevel;
import com.codeperf.server.model.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisTaskService {

    private final Map<String, AnalysisTask> tasks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisTask create(String project, String commit, String branch, String env) {
        String id = UUID.randomUUID().toString();
        AnalysisTask task = new AnalysisTask(id, project, commit, branch, env);
        tasks.put(id, task);
        return task;
    }

    public AnalysisTask get(String taskId) {
        AnalysisTask task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("analysis task not found: " + taskId);
        }
        return task;
    }

    public AnalysisTask acceptStaticResult(String taskId, String payload) {
        AnalysisTask task = get(taskId);
        RiskLevel staticRisk = deriveStaticRisk(payload);
        task.setStaticPayload(payload);
        task.setStaticRiskLevel(staticRisk);
        task.setRiskLevel(RiskLevel.max(task.getRiskLevel(), staticRisk));
        task.setStatus(TaskStatus.STATIC_RECEIVED);
        return task;
    }

    public AnalysisTask acceptDynamicEvidence(String taskId, String payload) {
        AnalysisTask task = get(taskId);
        task.setDynamicPayload(payload);
        task.setStatus(TaskStatus.DYNAMIC_RECEIVED);
        return task;
    }

    private RiskLevel deriveStaticRisk(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            JsonNode findings = root.path("findings");
            if (!findings.isArray() || findings.size() == 0) {
                return RiskLevel.NONE;
            }
            RiskLevel max = RiskLevel.INFO;
            for (JsonNode finding : findings) {
                String severity = finding.path("severity").asText("WARN");
                RiskLevel candidate = parseRisk(severity);
                max = RiskLevel.max(max, candidate);
            }
            return max;
        } catch (IOException e) {
            return RiskLevel.WARN;
        }
    }

    private RiskLevel parseRisk(String severity) {
        try {
            return RiskLevel.valueOf(severity.toUpperCase());
        } catch (Exception ignored) {
            return RiskLevel.WARN;
        }
    }
}
