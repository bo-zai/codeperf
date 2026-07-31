package com.cmb.codeperf.server.service.impl;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.AnalysisTaskCreateBO;
import com.cmb.codeperf.server.model.bo.DynamicCallEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicRequestEvidenceBO;
import com.cmb.codeperf.server.model.bo.FindingIssueBO;
import com.cmb.codeperf.server.model.bo.FindingOccurrenceBO;
import com.cmb.codeperf.server.model.bo.RiskLevel;
import com.cmb.codeperf.server.model.bo.StaticDynamicCorroborationBO;
import com.cmb.codeperf.server.model.bo.StaticFindingBO;
import com.cmb.codeperf.server.model.bo.TaskStatus;
import com.cmb.codeperf.server.model.dto.response.StaticFindingSummary;
import com.cmb.codeperf.server.model.dto.response.StaticReportSummary;
import com.cmb.codeperf.server.model.vo.report.RuntimeCorroborationSummaryVO;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import com.cmb.codeperf.server.util.RepositoryUrlNormalizer;
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
    private final RuntimeEvidenceAggregator runtimeEvidenceAggregator;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisTaskService(AnalysisTaskRepository repository,
                               StaticReportSummarizer staticReportSummarizer,
                               RuntimeEvidenceAggregator runtimeEvidenceAggregator) {
        this.repository = repository;
        this.staticReportSummarizer = staticReportSummarizer;
        this.runtimeEvidenceAggregator = runtimeEvidenceAggregator;
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
        if (hasStableTaskIdentity(command)) {
            java.util.Optional<AnalysisTaskBO> existing = repository.findByCommitIdentity(
                    command.getRemoteUrl(), command.getCommit(), command.getBranch(), command.getEnv());
            if (existing.isPresent()) {
                AnalysisTaskBO task = existing.get();
                log.info("静态任务已存在，复用已有任务 taskId={} project={} remoteUrl={} commit={} branch={} env={}",
                        task.getAnalysisTaskId(),
                        task.getProject(),
                        task.getRemoteUrl(),
                        task.getCommit(),
                        task.getBranch(),
                        task.getEnv());
                return task;
            }
        }
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
        AnalysisTaskBO saved = repository.save(task);
        log.info("静态任务创建完成 taskId={} project={} remoteUrl={} commit={} branch={} env={} authorEmail={}",
                saved.getAnalysisTaskId(),
                saved.getProject(),
                saved.getRemoteUrl(),
                saved.getCommit(),
                saved.getBranch(),
                saved.getEnv(),
                saved.getAuthorEmail());
        return saved;
    }

    public AnalysisTaskBO get(String taskId) {
        return repository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("analysis task not found: " + taskId));
    }

    public AnalysisTaskBO acceptStaticResult(String taskId, String payload) {
        long startNanos = System.nanoTime();
        AnalysisTaskBO task = get(taskId);
        log.info("静态报告接收身份 taskId={} project={} remoteUrl={} commit={} branch={} env={} authorEmail={}",
                task.getAnalysisTaskId(),
                task.getProject(),
                task.getRemoteUrl(),
                task.getCommit(),
                task.getBranch(),
                task.getEnv(),
                task.getAuthorEmail());
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

    private boolean hasStableTaskIdentity(AnalysisTaskCreateBO command) {
        return command != null
                && !isBlank(command.getRemoteUrl())
                && !isBlank(command.getCommit())
                && !isBlank(command.getBranch())
                && !isBlank(command.getEnv());
    }

    public AnalysisTaskBO acceptDynamicEvidence(String taskId, String payload) {
        AnalysisTaskBO task = get(taskId);
        task.setDynamicPayload(payload);
        task.setStatus(TaskStatus.DYNAMIC_RECEIVED);
        AnalysisTaskBO saved = repository.save(task);
        DynamicEvidenceBO rawEvidence = repository.appendDynamicEvidence(extractDynamicEvidence(task, payload));
        appendStructuredDynamicEvidence(saved, rawEvidence);
        return saved;
    }

    /**
     * 按构建身份接收动态证据。
     * 这样 agent 配置不需要每次注入 taskId，适配企业自动化 CI/CD 的固定启动参数。
     */
    public AnalysisTaskBO acceptDynamicEvidenceByIdentity(String payload) {
        DynamicEvidenceIdentity identity = parseDynamicEvidenceIdentity(payload);
        log.info("动态证据匹配身份 appName={} remoteUrl={} commit={} branch={} env={}",
                identity.appName,
                identity.remoteUrl,
                identity.commit,
                identity.branch,
                identity.env);
        java.util.Optional<AnalysisTaskBO> matched = repository.findByCommitIdentity(
                identity.remoteUrl, identity.commit, identity.branch, identity.env);
        if (!matched.isPresent()) {
            log.warn("动态证据未匹配到静态任务 appName={} remoteUrl={} commit={} branch={} env={}",
                    identity.appName,
                    identity.remoteUrl,
                    identity.commit,
                    identity.branch,
                    identity.env);
            throw new IllegalArgumentException("analysis task not found for dynamic evidence identity");
        }
        AnalysisTaskBO task = matched.get();
        log.info("动态证据匹配成功 taskId={} appName={} remoteUrl={} commit={} branch={} env={}",
                task.getAnalysisTaskId(),
                identity.appName,
                identity.remoteUrl,
                identity.commit,
                identity.branch,
                identity.env);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private void appendStructuredDynamicEvidence(AnalysisTaskBO task, DynamicEvidenceBO rawEvidence) {
        // 动态 raw JSON 只适合审计追溯，企业报告和统计必须落到请求、调用、佐证三个稳定维度。
        List<DynamicRequestEvidenceBO> requests = extractDynamicRequests(task, rawEvidence);
        for (DynamicRequestEvidenceBO request : requests) {
            repository.appendDynamicRequestEvidence(request);
            for (DynamicCallEvidenceBO call : extractDynamicCalls(request)) {
                call.setRequestEvidenceId(request.getId());
                repository.appendDynamicCallEvidence(call);
            }
        }
        refreshStaticDynamicCorroborations(task);
    }

    private List<DynamicRequestEvidenceBO> extractDynamicRequests(AnalysisTaskBO task, DynamicEvidenceBO rawEvidence) {
        try {
            JsonNode root = mapper.readTree(rawEvidence.getRawPayload());
            JsonNode requests = root.path("evidence").path("requests");
            if (!requests.isArray()) {
                return Collections.emptyList();
            }
            List<DynamicRequestEvidenceBO> result = new ArrayList<>();
            for (JsonNode request : requests) {
                DynamicRequestEvidenceBO evidence = new DynamicRequestEvidenceBO();
                evidence.setTaskId(task.getAnalysisTaskId());
                evidence.setRawEvidenceId(rawEvidence.getId());
                evidence.setEnv(task.getEnv());
                evidence.setAppName(rawEvidence.getAppName());
                evidence.setEntryMethod(text(request, "httpMethod"));
                evidence.setEntryPath(text(request, "path"));
                evidence.setEntryKey(requestEntryKey(request, rawEvidence.getEntryKey()));
                evidence.setWallTimeMs(request.path("wallTimeMs").asLong(0L));
                evidence.setRawPayload(request.toString());
                result.add(evidence);
            }
            return result;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private List<DynamicCallEvidenceBO> extractDynamicCalls(DynamicRequestEvidenceBO requestEvidence) {
        try {
            JsonNode root = mapper.readTree(requestEvidence.getRawPayload());
            List<DynamicCallEvidenceBO> result = new ArrayList<>();
            collectDynamicCalls(root.path("callTree"), requestEvidence, new ArrayList<String>(), result);
            return result;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private void refreshStaticDynamicCorroborations(AnalysisTaskBO task) {
        // 每次动态证据到达后重算当前任务的佐证摘要，保证多次接口调用能累积为整体判断。
        List<StaticFindingBO> staticFindings = repository.listStaticFindings(task.getAnalysisTaskId());
        if (staticFindings.isEmpty() || task.getStaticPayload() == null || task.getStaticPayload().trim().isEmpty()) {
            repository.replaceStaticDynamicCorroborations(task.getAnalysisTaskId(),
                    Collections.<StaticDynamicCorroborationBO>emptyList());
            return;
        }
        StaticReportSummary staticReport = staticReportSummarizer.summarize(task.getStaticPayload());
        List<StaticFindingSummary> summaries = staticReport == null
                ? Collections.<StaticFindingSummary>emptyList()
                : staticReport.getFindings();
        List<DynamicEvidenceBO> dynamicRecords = repository.listDynamicEvidence(task.getAnalysisTaskId());
        java.util.Map<String, RuntimeCorroborationSummaryVO> summaryMap =
                runtimeEvidenceAggregator.aggregate(summaries, dynamicRecords);
        List<StaticDynamicCorroborationBO> records = new ArrayList<>();
        for (StaticFindingBO staticFinding : staticFindings) {
            StaticFindingSummary summary = toStaticFindingSummary(staticFinding);
            RuntimeCorroborationSummaryVO runtimeSummary =
                    summaryMap.get(runtimeEvidenceAggregator.findingKey(summary));
            if (runtimeSummary == null) {
                runtimeSummary = new RuntimeCorroborationSummaryVO();
                runtimeSummary.setStatus("NOT_HIT");
                runtimeSummary.setText("当前没有找到能够直接对应这条静态风险的运行证据。");
            }
            records.add(toStaticDynamicCorroboration(task, staticFinding, runtimeSummary,
                    repository.findIssueByIssueKey(issueKey(task, staticFinding)).orElse(null)));
        }
        repository.replaceStaticDynamicCorroborations(task.getAnalysisTaskId(), records);
    }

    private StaticFindingSummary toStaticFindingSummary(StaticFindingBO finding) {
        try {
            // 匹配逻辑依赖静态 evidence、loopCallLine、ioLine 等细节，优先从原始 finding 还原完整摘要。
            JsonNode raw = mapper.readTree(finding.getRawPayload());
            return new StaticFindingSummary(
                    text(raw, "ruleId"),
                    text(raw, "severity"),
                    text(raw, "confidence"),
                    text(raw, "sourceFile"),
                    text(raw, "evidence"),
                    raw.path("lineNumber").asInt(0),
                    raw.path("loopStartLine").asInt(0),
                    raw.path("loopEndLine").asInt(0),
                    text(raw, "ioType"),
                    text(raw, "loopMethodName"),
                    raw.path("loopCallLine").asInt(0),
                    raw.path("ioLine").asInt(0),
                    null);
        } catch (IOException e) {
            return new StaticFindingSummary(
                    finding.getRuleId(),
                    finding.getSeverity(),
                    finding.getConfidence(),
                    finding.getSourceFile(),
                    "",
                    finding.getLineNumber(),
                    finding.getLoopStartLine(),
                    finding.getLoopEndLine(),
                    finding.getIoType(),
                    finding.getLoopMethodName(),
                    0,
                    0,
                    null);
        }
    }

    private StaticDynamicCorroborationBO toStaticDynamicCorroboration(AnalysisTaskBO task,
                                                                      StaticFindingBO staticFinding,
                                                                      RuntimeCorroborationSummaryVO runtimeSummary,
                                                                      FindingIssueBO findingIssue) {
        StaticDynamicCorroborationBO record = new StaticDynamicCorroborationBO();
        record.setTaskId(task.getAnalysisTaskId());
        record.setStaticFindingId(staticFinding.getId());
        record.setFindingIssueId(findingIssue == null ? null : findingIssue.getId());
        record.setStatus(runtimeSummary.getStatus());
        record.setHitRequestCount(runtimeSummary.getHitRequestCount());
        record.setHitEntryCount(runtimeSummary.getHitEntryCount());
        record.setMaxRepeatCount(runtimeSummary.getMaxRepeatCount());
        record.setAvgRepeatCount(runtimeSummary.getAvgRepeatCount());
        record.setTopEntryKey(runtimeSummary.getTopEntryKey());
        record.setReason(runtimeSummary.getText());
        return record;
    }

    private void collectDynamicCalls(JsonNode node,
                                     DynamicRequestEvidenceBO requestEvidence,
                                     List<String> path,
                                     List<DynamicCallEvidenceBO> result) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        String fullMethodName = text(node, "method");
        if (!fullMethodName.trim().isEmpty() && !"ROOT".equals(fullMethodName)) {
            List<String> nextPath = new ArrayList<>(path);
            nextPath.add(simpleMethodDisplay(fullMethodName));
            DynamicCallEvidenceBO call = new DynamicCallEvidenceBO();
            call.setTaskId(requestEvidence.getTaskId());
            call.setRawEvidenceId(requestEvidence.getRawEvidenceId());
            call.setEntryKey(requestEvidence.getEntryKey());
            call.setFullMethodName(fullMethodName);
            call.setClassName(className(fullMethodName));
            call.setMethodName(methodName(fullMethodName));
            call.setCallPath(joinPath(nextPath));
            call.setCallCount(node.path("count").asInt(0));
            call.setTotalTimeMs(node.path("totalTimeMs").asLong(node.path("totalMs").asLong(0L)));
            call.setRawPayload(node.toString());
            result.add(call);
            collectChildren(node, requestEvidence, nextPath, result);
            return;
        }
        collectChildren(node, requestEvidence, path, result);
    }

    private void collectChildren(JsonNode node,
                                 DynamicRequestEvidenceBO requestEvidence,
                                 List<String> path,
                                 List<DynamicCallEvidenceBO> result) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            collectDynamicCalls(child, requestEvidence, path, result);
        }
    }

    private String requestEntryKey(JsonNode request, String fallback) {
        String method = text(request, "httpMethod");
        String path = text(request, "path");
        String entryKey = (method + " " + path).trim();
        return entryKey.trim().isEmpty() ? fallback : entryKey;
    }

    private String simpleMethodDisplay(String fullMethod) {
        String className = className(fullMethod);
        String methodName = methodName(fullMethod);
        return className.trim().isEmpty() ? methodName : className + "." + methodName;
    }

    private String className(String fullMethod) {
        if (fullMethod == null) {
            return "";
        }
        int lastDot = fullMethod.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        int classStart = fullMethod.lastIndexOf('.', lastDot - 1);
        return classStart >= 0 ? fullMethod.substring(classStart + 1, lastDot) : fullMethod.substring(0, lastDot);
    }

    private String methodName(String fullMethod) {
        if (fullMethod == null) {
            return "";
        }
        int lastDot = fullMethod.lastIndexOf('.');
        return lastDot >= 0 ? fullMethod.substring(lastDot + 1) : fullMethod;
    }

    private String joinPath(List<String> path) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(path.get(i));
        }
        return builder.toString();
    }

    private DynamicEvidenceIdentity parseDynamicEvidenceIdentity(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            DynamicEvidenceIdentity identity = new DynamicEvidenceIdentity();
            identity.appName = text(root, "appName");
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
        return sha256(RepositoryUrlNormalizer.toRepoKey(task.getRemoteUrl())
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
        private String appName;
        private String remoteUrl;
        private String commit;
        private String branch;
        private String env;
    }
}

