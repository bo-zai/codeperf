package com.codeperf.server.service.impl;

import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.model.bo.AnalysisTaskCreateBO;
import com.codeperf.server.model.bo.DynamicEvidenceBO;
import com.codeperf.server.model.bo.RiskLevel;
import com.codeperf.server.model.bo.StaticFindingBO;
import com.codeperf.server.model.bo.TaskStatus;
import com.codeperf.server.service.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisTaskService {

    private final AnalysisTaskRepository repository;
    private final StaticReportSummarizer staticReportSummarizer;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisTaskService(AnalysisTaskRepository repository, StaticReportSummarizer staticReportSummarizer) {
        this.repository = repository;
        this.staticReportSummarizer = staticReportSummarizer;
    }

    public AnalysisTaskBO create(String project, String commit, String branch, String env) {
        AnalysisTaskCreateBO command = new AnalysisTaskCreateBO();
        command.setProject(project);
        command.setCommit(commit);
        command.setBranch(branch);
        command.setEnv(env);
        return create(command);
    }

    public AnalysisTaskBO create(AnalysisTaskCreateBO command) {
        String id = UUID.randomUUID().toString();
        AnalysisTaskBO task = new AnalysisTaskBO(
                id,
                valueOrEmpty(command.getProject()),
                valueOrEmpty(command.getRemoteUrl()),
                valueOrEmpty(command.getCommit()),
                valueOrEmpty(command.getBranch()),
                valueOrEmpty(command.getEnv()),
                valueOrEmpty(command.getAuthorName()),
                valueOrEmpty(command.getAuthorEmail()),
                valueOrEmpty(command.getAuthorTime()),
                valueOrEmpty(command.getCommitterName()),
                valueOrEmpty(command.getCommitterEmail()),
                valueOrEmpty(command.getCommitMessage()));
        return repository.save(task);
    }

    public AnalysisTaskBO get(String taskId) {
        return repository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("analysis task not found: " + taskId));
    }

    public AnalysisTaskBO acceptStaticResult(String taskId, String payload) {
        AnalysisTaskBO task = get(taskId);
        staticReportSummarizer.validate(payload);
        RiskLevel staticRisk = deriveStaticRisk(payload);
        task.setStaticPayload(payload);
        task.setStaticRiskLevel(staticRisk);
        task.setRiskLevel(RiskLevel.max(task.getRiskLevel(), staticRisk));
        task.setStatus(TaskStatus.STATIC_RECEIVED);
        AnalysisTaskBO saved = repository.save(task);
        repository.replaceStaticFindings(taskId, extractStaticFindings(taskId, payload));
        return saved;
    }

    public AnalysisTaskBO acceptDynamicEvidence(String taskId, String payload) {
        AnalysisTaskBO task = get(taskId);
        task.setDynamicPayload(payload);
        task.setStatus(TaskStatus.DYNAMIC_RECEIVED);
        AnalysisTaskBO saved = repository.save(task);
        repository.appendDynamicEvidence(extractDynamicEvidence(task, payload));
        return saved;
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<StaticFindingBO> extractStaticFindings(String taskId, String payload) {
        List<StaticFindingBO> records = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(payload);
            JsonNode findings = root.path("findings");
            if (!findings.isArray()) {
                return records;
            }
            for (JsonNode finding : findings) {
                StaticFindingBO record = new StaticFindingBO();
                record.setTaskId(taskId);
                record.setRuleId(text(finding, "type"));
                record.setSeverity(text(finding, "severity"));
                record.setConfidence(text(finding, "confidence"));
                record.setSourceFile(text(finding, "sourceFile"));
                record.setLineNumber(finding.path("lineNumber").asInt(0));
                record.setLoopStartLine(finding.path("loopStartLine").asInt(0));
                record.setLoopEndLine(finding.path("loopEndLine").asInt(0));
                record.setLoopMethodName(text(finding, "loopMethodName"));
                record.setIoType(text(finding, "ioType"));
                JsonNode attribution = finding.path("attribution");
                record.setRiskScope(text(attribution, "riskScope"));
                record.setChangedLine(attribution.path("changedLine").asBoolean(false));
                record.setIntroducedByName(text(attribution, "introducedByName"));
                record.setIntroducedByEmail(text(attribution, "introducedByEmail"));
                record.setIntroducedCommit(text(attribution, "introducedCommit"));
                record.setIntroducedCommitTime(text(attribution, "introducedCommitTime"));
                record.setRawPayload(finding.toString());
                record.setEvidenceHash(sha256(finding.toString()));
                records.add(record);
            }
            return records;
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid static result json", e);
        }
    }

    private DynamicEvidenceBO extractDynamicEvidence(AnalysisTaskBO task, String payload) {
        DynamicEvidenceBO record = new DynamicEvidenceBO();
        record.setTaskId(task.getAnalysisTaskId());
        record.setEnv(task.getEnv());
        record.setRawPayload(payload);
        try {
            JsonNode root = mapper.readTree(payload);
            record.setEntryKey(text(root, "entry"));
            record.setAppName(text(root, "appName"));
        } catch (IOException ignored) {
            record.setEntryKey("");
            record.setAppName("");
        }
        return record;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
