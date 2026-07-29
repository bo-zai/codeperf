package com.cmb.codeperf.server.service.impl;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.AnalysisTaskCreateBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.FindingIssueBO;
import com.cmb.codeperf.server.model.bo.FindingOccurrenceBO;
import com.cmb.codeperf.server.model.bo.RiskLevel;
import com.cmb.codeperf.server.model.bo.StaticFindingBO;
import com.cmb.codeperf.server.model.bo.TaskStatus;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
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
        long startNanos = System.nanoTime();
        AnalysisTaskBO task = get(taskId);
        long getTaskNanos = System.nanoTime();
        staticReportSummarizer.validate(payload);
        long validateNanos = System.nanoTime();
        RiskLevel staticRisk = deriveStaticRisk(payload);
        long deriveRiskNanos = System.nanoTime();
        task.setStaticPayload(payload);
        task.setStaticRiskLevel(staticRisk);
        task.setRiskLevel(RiskLevel.max(task.getRiskLevel(), staticRisk));
        task.setStatus(TaskStatus.STATIC_RECEIVED);
        List<StaticFindingBO> findings = extractStaticFindings(taskId, payload);
        long extractFindingNanos = System.nanoTime();
        validateRuleDefinitions(findings);
        long validateRuleNanos = System.nanoTime();
        AnalysisTaskBO saved = repository.save(task);
        long saveTaskNanos = System.nanoTime();
        repository.replaceStaticFindings(taskId, findings);
        long replaceFindingNanos = System.nanoTime();
        List<String> scannedSourceFiles = extractScannedSourceFiles(payload, findings);
        long extractScannedFileNanos = System.nanoTime();
        syncFindingIssues(saved, findings, scannedSourceFiles);
        long syncIssueNanos = System.nanoTime();
        log.info("静态报告处理完成 taskId={} findingCount={} scannedFileCount={} staticRisk={} "
                        + "getTaskMs={} validateMs={} deriveRiskMs={} extractFindingMs={} validateRuleMs={} "
                        + "saveTaskMs={} replaceFindingMs={} extractScannedFileMs={} syncIssueMs={} totalMs={}",
                taskId,
                findings.size(),
                scannedSourceFiles.size(),
                staticRisk,
                elapsedMs(startNanos, getTaskNanos),
                elapsedMs(getTaskNanos, validateNanos),
                elapsedMs(validateNanos, deriveRiskNanos),
                elapsedMs(deriveRiskNanos, extractFindingNanos),
                elapsedMs(extractFindingNanos, validateRuleNanos),
                elapsedMs(validateRuleNanos, saveTaskNanos),
                elapsedMs(saveTaskNanos, replaceFindingNanos),
                elapsedMs(replaceFindingNanos, extractScannedFileNanos),
                elapsedMs(extractScannedFileNanos, syncIssueNanos),
                elapsedMs(startNanos, syncIssueNanos));
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

    /**
     * 按构建身份接收动态证据。
     * 这样 agent 配置不需要每次注入 taskId，适配企业自动化 CI/CD 的固定启动参数。
     */
    public AnalysisTaskBO acceptDynamicEvidenceByIdentity(String payload) {
        DynamicEvidenceIdentity identity = parseDynamicEvidenceIdentity(payload);
        AnalysisTaskBO task = repository.findLatestByCommitIdentity(
                        identity.remoteUrl, identity.commit, identity.branch, identity.env)
                .orElseThrow(() -> new IllegalArgumentException("analysis task not found for dynamic evidence identity"));
        return acceptDynamicEvidence(task.getAnalysisTaskId(), payload);
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
                record.setRuleId(requiredText(finding, "ruleId"));
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
                record.setEvidenceHash(stableEvidenceHash(finding));
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
            record.setEntryKey(extractEntryKey(root));
            record.setAppName(text(root, "appName"));
        } catch (IOException ignored) {
            record.setEntryKey("");
            record.setAppName("");
        }
        return record;
    }

    private DynamicEvidenceIdentity parseDynamicEvidenceIdentity(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            DynamicEvidenceIdentity identity = new DynamicEvidenceIdentity();
            identity.remoteUrl = requiredText(root, "remoteUrl");
            identity.commit = requiredText(root, "commit");
            identity.branch = requiredText(root, "branch");
            identity.env = requiredText(root, "env");
            return identity;
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid dynamic evidence json", e);
        }
    }

    private String extractEntryKey(JsonNode root) {
        String direct = text(root, "entry");
        if (!direct.trim().isEmpty()) {
            return direct;
        }
        JsonNode evidence = root.path("evidence");
        String method = text(evidence, "entryMethod");
        String path = text(evidence, "entryPath");
        if (method.trim().isEmpty() && path.trim().isEmpty()) {
            return "";
        }
        return (method + " " + path).trim();
    }

    private void validateRuleDefinitions(List<StaticFindingBO> findings) {
        for (StaticFindingBO finding : findings) {
            if (!repository.isRuleDefined(finding.getRuleId())) {
                throw new IllegalArgumentException("undefined static ruleId: " + finding.getRuleId());
            }
        }
    }

    private void syncFindingIssues(AnalysisTaskBO task, List<StaticFindingBO> findings, List<String> scannedSourceFiles) {
        List<String> currentIssueKeys = new ArrayList<>();
        for (StaticFindingBO finding : findings) {
            String issueKey = issueKey(task, finding);
            currentIssueKeys.add(issueKey);
            FindingIssueBO issue = repository.saveOrUpdateIssue(task, finding, issueKey);
            FindingOccurrenceBO occurrence = new FindingOccurrenceBO();
            occurrence.setIssueId(issue.getId());
            occurrence.setTaskId(task.getAnalysisTaskId());
            occurrence.setOccurrenceType("NEW".equals(finding.getRiskScope()) ? "NEW" : "EXISTING");
            occurrence.setRiskScope(finding.getRiskScope());
            occurrence.setSeverity(finding.getSeverity());
            occurrence.setConfidence(finding.getConfidence());
            occurrence.setRawPayload(finding.getRawPayload());
            repository.appendFindingOccurrence(occurrence);
        }
        repository.closeResolvedIssues(task, scannedSourceFiles, currentIssueKeys);
    }

    private String issueKey(AnalysisTaskBO task, StaticFindingBO finding) {
        return sha256(valueOrEmpty(task.getRemoteUrl()).toLowerCase()
                + "|" + valueOrEmpty(task.getBranch())
                + "|" + valueOrEmpty(finding.getRuleId())
                + "|" + valueOrEmpty(finding.getSourceFile()).replace('\\', '/')
                + "|" + valueOrEmpty(finding.getLoopMethodName())
                + "|" + valueOrEmpty(finding.getEvidenceHash()));
    }

    private String stableEvidenceHash(JsonNode finding) {
        return sha256(text(finding, "ruleId")
                + "|" + text(finding, "sourceFile").replace('\\', '/')
                + "|" + text(finding, "loopMethodName")
                + "|" + text(finding, "ioType")
                + "|" + text(finding, "evidence"));
    }

    private List<String> extractScannedSourceFiles(String payload, List<StaticFindingBO> findings) {
        try {
            JsonNode root = mapper.readTree(payload);
            JsonNode scannedSourceFiles = root.path("scannedSourceFiles");
            if (scannedSourceFiles.isArray()) {
                List<String> files = new ArrayList<>();
                for (JsonNode file : scannedSourceFiles) {
                    String value = file.asText("");
                    if (!value.trim().isEmpty()) {
                        files.add(normalizeSourceFile(value));
                    }
                }
                return files;
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        // 旧版本报告没有 scannedSourceFiles，只能认为 finding 所在文件被扫描过，避免空 findings 误关全分支。
        List<String> files = new ArrayList<>();
        for (StaticFindingBO finding : findings) {
            String sourceFile = normalizeSourceFile(finding.getSourceFile());
            if (!sourceFile.isEmpty() && !files.contains(sourceFile)) {
                files.add(sourceFile);
            }
        }
        return files;
    }

    private String normalizeSourceFile(String sourceFile) {
        return sourceFile == null ? "" : sourceFile.trim().replace('\\', '/');
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("static finding missing required field: " + field);
        }
        return value;
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

    private long elapsedMs(long startNanos, long endNanos) {
        return (endNanos - startNanos) / 1_000_000L;
    }

    private static final class DynamicEvidenceIdentity {
        private String remoteUrl;
        private String commit;
        private String branch;
        private String env;
    }
}

