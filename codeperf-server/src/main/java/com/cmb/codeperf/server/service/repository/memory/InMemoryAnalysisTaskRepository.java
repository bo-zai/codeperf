package com.cmb.codeperf.server.service.repository.memory;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.DynamicCallEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicRequestEvidenceBO;
import com.cmb.codeperf.server.model.bo.FindingIssueBO;
import com.cmb.codeperf.server.model.bo.FindingOccurrenceBO;
import com.cmb.codeperf.server.model.bo.StaticDynamicCorroborationBO;
import com.cmb.codeperf.server.model.bo.StaticFindingBO;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, AnalysisTaskBO> tasks = new ConcurrentHashMap<>();
    private final List<String> taskOrder = new CopyOnWriteArrayList<>();
    private final Map<String, List<StaticFindingBO>> staticFindings = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicEvidenceBO>> dynamicEvidence = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicRequestEvidenceBO>> dynamicRequestEvidence = new ConcurrentHashMap<>();
    private final Map<String, List<DynamicCallEvidenceBO>> dynamicCallEvidence = new ConcurrentHashMap<>();
    private final Map<String, List<StaticDynamicCorroborationBO>> staticDynamicCorroborations = new ConcurrentHashMap<>();
    private final Map<String, FindingIssueBO> issues = new ConcurrentHashMap<>();
    private final List<FindingOccurrenceBO> occurrences = new CopyOnWriteArrayList<>();
    private final Set<String> ruleIds = new HashSet<>(Arrays.asList("LOOP_IO_AMPLIFICATION"));

    @Override
    public AnalysisTaskBO save(AnalysisTaskBO task) {
        if (!tasks.containsKey(task.getAnalysisTaskId())) {
            taskOrder.add(task.getAnalysisTaskId());
        }
        task.setUpdatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER));
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
    public List<AnalysisTaskBO> listRecentTasks(int limit) {
        List<AnalysisTaskBO> result = new ArrayList<>();
        int max = Math.max(limit, 0);
        for (int i = taskOrder.size() - 1; i >= 0 && result.size() < max; i--) {
            AnalysisTaskBO task = tasks.get(taskOrder.get(i));
            if (task != null) {
                result.add(task);
            }
        }
        return result;
    }

    @Override
    public List<StaticFindingBO> listStaticFindings(String taskId) {
        return new ArrayList<>(staticFindings.getOrDefault(taskId, new ArrayList<StaticFindingBO>()));
    }

    @Override
    public List<DynamicEvidenceBO> listDynamicEvidence(String taskId) {
        return new ArrayList<>(dynamicEvidence.getOrDefault(taskId, new ArrayList<DynamicEvidenceBO>()));
    }

    @Override
    public void replaceStaticFindings(String taskId, List<StaticFindingBO> findings) {
        List<StaticFindingBO> stored = new ArrayList<>(findings.size());
        for (StaticFindingBO finding : findings) {
            finding.setId((long) staticFindings.values().stream().mapToInt(List::size).sum() + stored.size() + 1);
            stored.add(finding);
        }
        staticFindings.put(taskId, stored);
    }

    @Override
    public DynamicEvidenceBO appendDynamicEvidence(DynamicEvidenceBO evidence) {
        evidence.setId((long) dynamicEvidence.values().stream().mapToInt(List::size).sum() + 1);
        dynamicEvidence.computeIfAbsent(evidence.getTaskId(), ignored -> new ArrayList<>()).add(evidence);
        return evidence;
    }

    @Override
    public void appendDynamicRequestEvidence(DynamicRequestEvidenceBO evidence) {
        evidence.setId((long) dynamicRequestEvidence.values().stream().mapToInt(List::size).sum() + 1);
        dynamicRequestEvidence.computeIfAbsent(evidence.getTaskId(), ignored -> new ArrayList<>()).add(evidence);
    }

    @Override
    public void appendDynamicCallEvidence(DynamicCallEvidenceBO evidence) {
        evidence.setId((long) dynamicCallEvidence.values().stream().mapToInt(List::size).sum() + 1);
        dynamicCallEvidence.computeIfAbsent(evidence.getTaskId(), ignored -> new ArrayList<>()).add(evidence);
    }

    @Override
    public List<DynamicRequestEvidenceBO> listDynamicRequestEvidence(String taskId) {
        return new ArrayList<>(dynamicRequestEvidence.getOrDefault(taskId, new ArrayList<DynamicRequestEvidenceBO>()));
    }

    @Override
    public List<DynamicCallEvidenceBO> listDynamicCallEvidence(String taskId) {
        return new ArrayList<>(dynamicCallEvidence.getOrDefault(taskId, new ArrayList<DynamicCallEvidenceBO>()));
    }

    @Override
    public void replaceStaticDynamicCorroborations(String taskId, List<StaticDynamicCorroborationBO> corroborations) {
        List<StaticDynamicCorroborationBO> stored = new ArrayList<>(corroborations.size());
        for (StaticDynamicCorroborationBO corroboration : corroborations) {
            corroboration.setId((long) staticDynamicCorroborations.values().stream().mapToInt(List::size).sum()
                    + stored.size() + 1);
            stored.add(corroboration);
        }
        staticDynamicCorroborations.put(taskId, stored);
    }

    @Override
    public List<StaticDynamicCorroborationBO> listStaticDynamicCorroborations(String taskId) {
        return new ArrayList<>(staticDynamicCorroborations.getOrDefault(taskId,
                new ArrayList<StaticDynamicCorroborationBO>()));
    }

    @Override
    public boolean isRuleDefined(String ruleId) {
        return ruleIds.contains(ruleId);
    }

    @Override
    public FindingIssueBO saveOrUpdateIssue(AnalysisTaskBO task, StaticFindingBO finding, String issueKey) {
        FindingIssueBO issue = issues.get(issueKey);
        if (issue == null) {
            issue = new FindingIssueBO();
            issue.setId((long) issues.size() + 1);
            issue.setIssueKey(issueKey);
            issue.setRepositoryId((long) repoKey(task).hashCode());
            issue.setBranchName(task.getBranch());
            issue.setRuleId(finding.getRuleId());
            issue.setEvidenceHash(finding.getEvidenceHash());
            issue.setSourceFile(finding.getSourceFile());
            issue.setLineNumber(finding.getLineNumber());
            issue.setLoopMethodName(finding.getLoopMethodName());
            issue.setIoType(finding.getIoType());
            issue.setOwnerName(finding.getIntroducedByName());
            issue.setOwnerEmail(finding.getIntroducedByEmail());
            issue.setFirstSeenTaskId(task.getAnalysisTaskId());
        }
        issue.setStatus("OPEN");
        issue.setSeverity(finding.getSeverity());
        issue.setLastSeenTaskId(task.getAnalysisTaskId());
        issue.setRawPayload(finding.getRawPayload());
        issues.put(issueKey, issue);
        return issue;
    }

    @Override
    public Optional<FindingIssueBO> findIssueByIssueKey(String issueKey) {
        return Optional.ofNullable(issues.get(issueKey));
    }

    @Override
    public void appendFindingOccurrence(FindingOccurrenceBO occurrence) {
        occurrences.add(occurrence);
    }

    @Override
    public List<FindingIssueBO> listOpenIssues(String remoteUrl, String branch) {
        List<FindingIssueBO> result = new ArrayList<>();
        String key = repoKey(remoteUrl);
        for (FindingIssueBO issue : issues.values()) {
            if ("OPEN".equals(issue.getStatus())
                    && same(String.valueOf(issue.getRepositoryId()), String.valueOf((long) key.hashCode()))
                    && same(issue.getBranchName(), branch)) {
                result.add(issue);
            }
        }
        result.sort((left, right) -> firstNonBlank(left.getOwnerEmail()).compareTo(firstNonBlank(right.getOwnerEmail())));
        return result;
    }

    @Override
    public List<DynamicEvidenceBO> listLatestDynamicEvidence(String remoteUrl, String branch, String env) {
        AnalysisTaskBO latest = null;
        for (int i = taskOrder.size() - 1; i >= 0; i--) {
            AnalysisTaskBO task = tasks.get(taskOrder.get(i));
            if (task != null && same(task.getRemoteUrl(), remoteUrl) && same(task.getBranch(), branch)
                    && same(task.getEnv(), env) && !listDynamicEvidence(task.getAnalysisTaskId()).isEmpty()) {
                latest = task;
                break;
            }
        }
        return latest == null ? new ArrayList<DynamicEvidenceBO>() : listDynamicEvidence(latest.getAnalysisTaskId());
    }

    @Override
    public void closeResolvedIssues(AnalysisTaskBO task, List<String> scannedSourceFiles, List<String> currentIssueKeys) {
        if (scannedSourceFiles == null || scannedSourceFiles.isEmpty()) {
            return;
        }
        Set<String> scannedFiles = new HashSet<>();
        for (String sourceFile : scannedSourceFiles) {
            scannedFiles.add(normalizeSourceFile(sourceFile));
        }
        Set<String> currentKeys = new HashSet<>(currentIssueKeys);
        String repositoryId = String.valueOf((long) repoKey(task).hashCode());
        for (FindingIssueBO issue : issues.values()) {
            if (!"OPEN".equals(issue.getStatus())) {
                continue;
            }
            if (!same(String.valueOf(issue.getRepositoryId()), repositoryId) || !same(issue.getBranchName(), task.getBranch())) {
                continue;
            }
            if (!scannedFiles.contains(normalizeSourceFile(issue.getSourceFile())) || currentKeys.contains(issue.getIssueKey())) {
                continue;
            }
            issue.setStatus("FIXED");
            issue.setLastSeenTaskId(task.getAnalysisTaskId());
            FindingOccurrenceBO occurrence = new FindingOccurrenceBO();
            occurrence.setIssueId(issue.getId());
            occurrence.setTaskId(task.getAnalysisTaskId());
            occurrence.setOccurrenceType("FIXED");
            occurrence.setRiskScope("FIXED");
            occurrence.setSeverity(issue.getSeverity());
            occurrences.add(occurrence);
        }
    }

    private boolean same(String left, String right) {
        return value(left).equals(value(right));
    }

    private String repoKey(AnalysisTaskBO task) {
        return repoKey(task.getRemoteUrl());
    }

    private String repoKey(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty() || "UNKNOWN".equals(remoteUrl)) {
            return "";
        }
        return remoteUrl.trim().toLowerCase();
    }

    private String firstNonBlank(String value) {
        return value == null ? "" : value;
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSourceFile(String sourceFile) {
        return sourceFile == null ? "" : sourceFile.trim().replace('\\', '/');
    }
}

