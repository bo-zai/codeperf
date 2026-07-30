package com.cmb.codeperf.server.service.repository.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.DynamicCallEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicRequestEvidenceBO;
import com.cmb.codeperf.server.model.bo.FindingIssueBO;
import com.cmb.codeperf.server.model.bo.FindingOccurrenceBO;
import com.cmb.codeperf.server.model.bo.RiskLevel;
import com.cmb.codeperf.server.model.bo.StaticDynamicCorroborationBO;
import com.cmb.codeperf.server.model.bo.StaticFindingBO;
import com.cmb.codeperf.server.model.bo.TaskStatus;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import com.cmb.codeperf.server.model.entity.AnalysisTask;
import com.cmb.codeperf.server.model.entity.CodeRepository;
import com.cmb.codeperf.server.model.entity.DynamicCallEvidence;
import com.cmb.codeperf.server.model.entity.DynamicEvidence;
import com.cmb.codeperf.server.model.entity.DynamicRequestEvidence;
import com.cmb.codeperf.server.model.entity.FindingIssue;
import com.cmb.codeperf.server.model.entity.FindingOccurrence;
import com.cmb.codeperf.server.model.entity.GitCommit;
import com.cmb.codeperf.server.model.entity.RuleDefinition;
import com.cmb.codeperf.server.model.entity.StaticDynamicCorroboration;
import com.cmb.codeperf.server.model.entity.StaticFinding;
import com.cmb.codeperf.server.mapper.AnalysisTaskMapper;
import com.cmb.codeperf.server.mapper.CodeRepositoryMapper;
import com.cmb.codeperf.server.mapper.DynamicCallEvidenceMapper;
import com.cmb.codeperf.server.mapper.DynamicEvidenceMapper;
import com.cmb.codeperf.server.mapper.DynamicRequestEvidenceMapper;
import com.cmb.codeperf.server.mapper.FindingIssueMapper;
import com.cmb.codeperf.server.mapper.FindingOccurrenceMapper;
import com.cmb.codeperf.server.mapper.GitCommitMapper;
import com.cmb.codeperf.server.mapper.RuleDefinitionMapper;
import com.cmb.codeperf.server.mapper.StaticDynamicCorroborationMapper;
import com.cmb.codeperf.server.mapper.StaticFindingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
@ConditionalOnProperty(name = "codeperf.storage.mode", havingValue = "mysql")
public class MybatisPlusAnalysisTaskRepository implements AnalysisTaskRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalysisTaskMapper mapper;
    private final CodeRepositoryMapper repositoryMapper;
    private final GitCommitMapper gitCommitMapper;
    private final StaticFindingMapper staticFindingMapper;
    private final DynamicEvidenceMapper dynamicEvidenceMapper;
    private final DynamicRequestEvidenceMapper dynamicRequestEvidenceMapper;
    private final DynamicCallEvidenceMapper dynamicCallEvidenceMapper;
    private final StaticDynamicCorroborationMapper staticDynamicCorroborationMapper;
    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final FindingIssueMapper findingIssueMapper;
    private final FindingOccurrenceMapper findingOccurrenceMapper;

    public MybatisPlusAnalysisTaskRepository(AnalysisTaskMapper mapper,
                                             CodeRepositoryMapper repositoryMapper,
                                             GitCommitMapper gitCommitMapper,
                                             StaticFindingMapper staticFindingMapper,
                                             DynamicEvidenceMapper dynamicEvidenceMapper,
                                             DynamicRequestEvidenceMapper dynamicRequestEvidenceMapper,
                                             DynamicCallEvidenceMapper dynamicCallEvidenceMapper,
                                             StaticDynamicCorroborationMapper staticDynamicCorroborationMapper,
                                             RuleDefinitionMapper ruleDefinitionMapper,
                                             FindingIssueMapper findingIssueMapper,
                                             FindingOccurrenceMapper findingOccurrenceMapper) {
        this.mapper = mapper;
        this.repositoryMapper = repositoryMapper;
        this.gitCommitMapper = gitCommitMapper;
        this.staticFindingMapper = staticFindingMapper;
        this.dynamicEvidenceMapper = dynamicEvidenceMapper;
        this.dynamicRequestEvidenceMapper = dynamicRequestEvidenceMapper;
        this.dynamicCallEvidenceMapper = dynamicCallEvidenceMapper;
        this.staticDynamicCorroborationMapper = staticDynamicCorroborationMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.findingIssueMapper = findingIssueMapper;
        this.findingOccurrenceMapper = findingOccurrenceMapper;
    }

    @Override
    public AnalysisTaskBO save(AnalysisTaskBO task) {
        CodeRepository repository = saveRepository(task);
        GitCommit gitCommit = saveGitCommit(task, repository);
        AnalysisTask entity = toEntity(task, repository, gitCommit);
        AnalysisTask existing = findEntity(task.getAnalysisTaskId()).orElse(null);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            mapper.updateById(entity);
        }
        return task;
    }

    @Override
    public Optional<AnalysisTaskBO> findByTaskId(String taskId) {
        return findEntity(taskId).map(this::toDomain);
    }

    @Override
    public Optional<AnalysisTaskBO> findLatestByCommitIdentity(String remoteUrl, String commit, String branch, String env) {
        CodeRepository repository = findRepository(repoKey(remoteUrl)).orElse(null);
        if (repository == null) {
            return Optional.empty();
        }
        GitCommit gitCommit = findGitCommit(repository.getId(), commit, branch).orElse(null);
        if (gitCommit == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<AnalysisTask> query = new LambdaQueryWrapper<>();
        query.eq(AnalysisTask::getRepositoryId, repository.getId());
        query.eq(AnalysisTask::getGitCommitId, gitCommit.getId());
        query.eq(AnalysisTask::getEnvName, env);
        query.orderByDesc(AnalysisTask::getId);
        query.last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query)).map(this::toDomain);
    }

    @Override
    public List<AnalysisTaskBO> listRecentTasks(int limit) {
        LambdaQueryWrapper<AnalysisTask> query = new LambdaQueryWrapper<>();
        query.orderByDesc(AnalysisTask::getId);
        query.last("LIMIT " + Math.max(limit, 0));
        List<AnalysisTask> entities = mapper.selectList(query);
        List<AnalysisTaskBO> result = new java.util.ArrayList<>(entities.size());
        for (AnalysisTask entity : entities) {
            result.add(toDomain(entity));
        }
        return result;
    }

    @Override
    public List<StaticFindingBO> listStaticFindings(String taskId) {
        LambdaQueryWrapper<StaticFinding> query = new LambdaQueryWrapper<>();
        query.eq(StaticFinding::getTaskId, taskId);
        query.orderByAsc(StaticFinding::getSourceFile);
        query.orderByAsc(StaticFinding::getLineNumber);
        List<StaticFinding> entities = staticFindingMapper.selectList(query);
        List<StaticFindingBO> result = new java.util.ArrayList<>(entities.size());
        for (StaticFinding entity : entities) {
            result.add(toStaticFindingBO(entity));
        }
        return result;
    }

    @Override
    public List<DynamicEvidenceBO> listDynamicEvidence(String taskId) {
        LambdaQueryWrapper<DynamicEvidence> query = new LambdaQueryWrapper<>();
        query.eq(DynamicEvidence::getTaskId, taskId);
        query.orderByDesc(DynamicEvidence::getId);
        List<DynamicEvidence> entities = dynamicEvidenceMapper.selectList(query);
        List<DynamicEvidenceBO> result = new java.util.ArrayList<>(entities.size());
        for (DynamicEvidence entity : entities) {
            result.add(toDynamicEvidenceBO(entity));
        }
        return result;
    }

    @Override
    public void replaceStaticFindings(String taskId, List<StaticFindingBO> findings) {
        LambdaQueryWrapper<StaticFinding> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(StaticFinding::getTaskId, taskId);
        staticFindingMapper.delete(deleteQuery);
        for (StaticFindingBO finding : findings) {
            StaticFinding entity = toStaticFinding(finding);
            staticFindingMapper.insert(entity);
            finding.setId(entity.getId());
        }
    }

    @Override
    public DynamicEvidenceBO appendDynamicEvidence(DynamicEvidenceBO evidence) {
        DynamicEvidence entity = new DynamicEvidence();
        entity.setTaskId(evidence.getTaskId());
        entity.setEnvName(evidence.getEnv());
        entity.setAppName(evidence.getAppName());
        entity.setEntryKey(evidence.getEntryKey());
        entity.setRawPayload(evidence.getRawPayload());
        findByTaskId(evidence.getTaskId()).ifPresent(task -> {
            CodeRepository repository = saveRepository(task);
            GitCommit gitCommit = saveGitCommit(task, repository);
            entity.setRepositoryId(repository.getId());
            entity.setGitCommitId(gitCommit.getId());
        });
        dynamicEvidenceMapper.insert(entity);
        evidence.setId(entity.getId());
        return evidence;
    }

    @Override
    public void appendDynamicRequestEvidence(DynamicRequestEvidenceBO evidence) {
        DynamicRequestEvidence entity = toDynamicRequestEvidence(evidence);
        dynamicRequestEvidenceMapper.insert(entity);
        evidence.setId(entity.getId());
    }

    @Override
    public void appendDynamicCallEvidence(DynamicCallEvidenceBO evidence) {
        DynamicCallEvidence entity = toDynamicCallEvidence(evidence);
        dynamicCallEvidenceMapper.insert(entity);
        evidence.setId(entity.getId());
    }

    @Override
    public List<DynamicRequestEvidenceBO> listDynamicRequestEvidence(String taskId) {
        LambdaQueryWrapper<DynamicRequestEvidence> query = new LambdaQueryWrapper<>();
        query.eq(DynamicRequestEvidence::getTaskId, taskId);
        query.orderByAsc(DynamicRequestEvidence::getId);
        List<DynamicRequestEvidence> entities = dynamicRequestEvidenceMapper.selectList(query);
        List<DynamicRequestEvidenceBO> result = new java.util.ArrayList<>(entities.size());
        for (DynamicRequestEvidence entity : entities) {
            result.add(toDynamicRequestEvidenceBO(entity));
        }
        return result;
    }

    @Override
    public List<DynamicCallEvidenceBO> listDynamicCallEvidence(String taskId) {
        LambdaQueryWrapper<DynamicCallEvidence> query = new LambdaQueryWrapper<>();
        query.eq(DynamicCallEvidence::getTaskId, taskId);
        query.orderByAsc(DynamicCallEvidence::getId);
        List<DynamicCallEvidence> entities = dynamicCallEvidenceMapper.selectList(query);
        List<DynamicCallEvidenceBO> result = new java.util.ArrayList<>(entities.size());
        for (DynamicCallEvidence entity : entities) {
            result.add(toDynamicCallEvidenceBO(entity));
        }
        return result;
    }

    @Override
    public void replaceStaticDynamicCorroborations(String taskId, List<StaticDynamicCorroborationBO> corroborations) {
        LambdaQueryWrapper<StaticDynamicCorroboration> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(StaticDynamicCorroboration::getTaskId, taskId);
        staticDynamicCorroborationMapper.delete(deleteQuery);
        for (StaticDynamicCorroborationBO corroboration : corroborations) {
            StaticDynamicCorroboration entity = toStaticDynamicCorroboration(corroboration);
            staticDynamicCorroborationMapper.insert(entity);
            corroboration.setId(entity.getId());
        }
    }

    @Override
    public List<StaticDynamicCorroborationBO> listStaticDynamicCorroborations(String taskId) {
        LambdaQueryWrapper<StaticDynamicCorroboration> query = new LambdaQueryWrapper<>();
        query.eq(StaticDynamicCorroboration::getTaskId, taskId);
        query.orderByAsc(StaticDynamicCorroboration::getId);
        List<StaticDynamicCorroboration> entities = staticDynamicCorroborationMapper.selectList(query);
        List<StaticDynamicCorroborationBO> result = new java.util.ArrayList<>(entities.size());
        for (StaticDynamicCorroboration entity : entities) {
            result.add(toStaticDynamicCorroborationBO(entity));
        }
        return result;
    }

    @Override
    public boolean isRuleDefined(String ruleId) {
        LambdaQueryWrapper<RuleDefinition> query = new LambdaQueryWrapper<>();
        query.eq(RuleDefinition::getRuleId, ruleId);
        query.eq(RuleDefinition::getEnabled, Boolean.TRUE);
        query.last("LIMIT 1");
        return ruleDefinitionMapper.selectOne(query) != null;
    }

    @Override
    public FindingIssueBO saveOrUpdateIssue(AnalysisTaskBO task, StaticFindingBO finding, String issueKey) {
        CodeRepository repository = saveRepository(task);
        FindingIssue entity = findIssue(issueKey).orElse(null);
        if (entity == null) {
            entity = new FindingIssue();
            entity.setIssueKey(issueKey);
            entity.setRepositoryId(repository.getId());
            entity.setBranchName(task.getBranch());
            entity.setRuleId(finding.getRuleId());
            entity.setEvidenceHash(finding.getEvidenceHash());
            entity.setSourceFile(finding.getSourceFile());
            entity.setLineNumber(finding.getLineNumber());
            entity.setLoopMethodName(finding.getLoopMethodName());
            entity.setIoType(finding.getIoType());
            entity.setOwnerName(finding.getIntroducedByName());
            entity.setOwnerEmail(finding.getIntroducedByEmail());
            entity.setFirstSeenTaskId(task.getAnalysisTaskId());
            entity.setFirstSeenAt(LocalDateTime.now());
        }
        entity.setStatus("OPEN");
        entity.setSeverity(finding.getSeverity());
        entity.setLastSeenTaskId(task.getAnalysisTaskId());
        entity.setRawPayload(finding.getRawPayload());
        entity.setLastSeenAt(LocalDateTime.now());
        if (entity.getId() == null) {
            findingIssueMapper.insert(entity);
        } else {
            findingIssueMapper.updateById(entity);
        }
        return toFindingIssueBO(entity);
    }

    @Override
    public Optional<FindingIssueBO> findIssueByIssueKey(String issueKey) {
        return findIssue(issueKey).map(this::toFindingIssueBO);
    }

    @Override
    public void appendFindingOccurrence(FindingOccurrenceBO occurrence) {
        FindingOccurrence entity = new FindingOccurrence();
        entity.setIssueId(occurrence.getIssueId());
        entity.setTaskId(occurrence.getTaskId());
        entity.setOccurrenceType(occurrence.getOccurrenceType());
        entity.setRiskScope(occurrence.getRiskScope());
        entity.setSeverity(occurrence.getSeverity());
        entity.setConfidence(occurrence.getConfidence());
        entity.setRawPayload(occurrence.getRawPayload());
        FindingOccurrence existing = findOccurrence(occurrence.getIssueId(), occurrence.getTaskId()).orElse(null);
        if (existing == null) {
            findingOccurrenceMapper.insert(entity);
            return;
        }
        entity.setId(existing.getId());
        findingOccurrenceMapper.updateById(entity);
    }

    @Override
    public List<FindingIssueBO> listOpenIssues(String remoteUrl, String branch) {
        CodeRepository repository = findRepository(repoKey(remoteUrl)).orElse(null);
        if (repository == null) {
            return new java.util.ArrayList<>();
        }
        LambdaQueryWrapper<FindingIssue> query = new LambdaQueryWrapper<>();
        query.eq(FindingIssue::getRepositoryId, repository.getId());
        query.eq(FindingIssue::getBranchName, branch);
        query.eq(FindingIssue::getStatus, "OPEN");
        query.orderByAsc(FindingIssue::getOwnerEmail);
        query.orderByAsc(FindingIssue::getSourceFile);
        List<FindingIssue> entities = findingIssueMapper.selectList(query);
        List<FindingIssueBO> result = new java.util.ArrayList<>(entities.size());
        for (FindingIssue entity : entities) {
            result.add(toFindingIssueBO(entity));
        }
        return result;
    }

    @Override
    public List<DynamicEvidenceBO> listLatestDynamicEvidence(String remoteUrl, String branch, String env) {
        List<AnalysisTaskBO> tasks = listRecentTasks(100);
        for (AnalysisTaskBO task : tasks) {
            if (same(task.getRemoteUrl(), remoteUrl) && same(task.getBranch(), branch) && same(task.getEnv(), env)) {
                List<DynamicEvidenceBO> records = listDynamicEvidence(task.getAnalysisTaskId());
                if (!records.isEmpty()) {
                    return records;
                }
            }
        }
        return new java.util.ArrayList<>();
    }

    @Override
    public void closeResolvedIssues(AnalysisTaskBO task, List<String> scannedSourceFiles, List<String> currentIssueKeys) {
        if (scannedSourceFiles == null || scannedSourceFiles.isEmpty()) {
            return;
        }
        CodeRepository repository = saveRepository(task);
        Set<String> currentKeys = new HashSet<>(currentIssueKeys);
        List<String> normalizedSourceFiles = normalizeSourceFiles(scannedSourceFiles);
        if (normalizedSourceFiles.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<FindingIssue> query = new LambdaQueryWrapper<>();
        query.eq(FindingIssue::getRepositoryId, repository.getId());
        query.eq(FindingIssue::getBranchName, task.getBranch());
        query.eq(FindingIssue::getStatus, "OPEN");
        query.in(FindingIssue::getSourceFile, normalizedSourceFiles);
        List<FindingIssue> issues = findingIssueMapper.selectList(query);
        for (FindingIssue issue : issues) {
            if (currentKeys.contains(issue.getIssueKey())) {
                continue;
            }
            issue.setStatus("FIXED");
            issue.setFixedTaskId(task.getAnalysisTaskId());
            issue.setFixedAt(LocalDateTime.now());
            findingIssueMapper.updateById(issue);
            appendFixedOccurrence(task, issue);
        }
    }

    private Optional<AnalysisTask> findEntity(String taskId) {
        LambdaQueryWrapper<AnalysisTask> query = new LambdaQueryWrapper<>();
        query.eq(AnalysisTask::getTaskId, taskId);
        query.last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query));
    }

    private void appendFixedOccurrence(AnalysisTaskBO task, FindingIssue issue) {
        FindingOccurrenceBO occurrence = new FindingOccurrenceBO();
        occurrence.setIssueId(issue.getId());
        occurrence.setTaskId(task.getAnalysisTaskId());
        occurrence.setOccurrenceType("FIXED");
        occurrence.setRiskScope("FIXED");
        occurrence.setSeverity(issue.getSeverity());
        occurrence.setConfidence("");
        occurrence.setRawPayload(issue.getRawPayload());
        appendFindingOccurrence(occurrence);
    }

    private List<String> normalizeSourceFiles(List<String> scannedSourceFiles) {
        List<String> result = new java.util.ArrayList<>(scannedSourceFiles.size());
        for (String sourceFile : scannedSourceFiles) {
            String normalized = normalizeSourceFile(sourceFile);
            if (!normalized.isEmpty() && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalizeSourceFile(String sourceFile) {
        return sourceFile == null ? "" : sourceFile.trim().replace('\\', '/');
    }

    private Optional<FindingIssue> findIssue(String issueKey) {
        LambdaQueryWrapper<FindingIssue> query = new LambdaQueryWrapper<>();
        query.eq(FindingIssue::getIssueKey, issueKey);
        query.last("LIMIT 1");
        return Optional.ofNullable(findingIssueMapper.selectOne(query));
    }

    private Optional<FindingOccurrence> findOccurrence(Long issueId, String taskId) {
        LambdaQueryWrapper<FindingOccurrence> query = new LambdaQueryWrapper<>();
        query.eq(FindingOccurrence::getIssueId, issueId);
        query.eq(FindingOccurrence::getTaskId, taskId);
        query.last("LIMIT 1");
        return Optional.ofNullable(findingOccurrenceMapper.selectOne(query));
    }

    private AnalysisTask toEntity(AnalysisTaskBO task, CodeRepository repository, GitCommit gitCommit) {
        AnalysisTask entity = new AnalysisTask();
        entity.setTaskId(task.getAnalysisTaskId());
        entity.setRepositoryId(repository.getId());
        entity.setGitCommitId(gitCommit.getId());
        entity.setEnvName(task.getEnv());
        entity.setStatus(task.getStatus().name());
        entity.setRiskLevel(task.getRiskLevel().name());
        entity.setStaticRiskLevel(task.getStaticRiskLevel().name());
        entity.setStaticPayload(task.getStaticPayload());
        entity.setDynamicPayload(task.getDynamicPayload());
        return entity;
    }

    private StaticFinding toStaticFinding(StaticFindingBO finding) {
        StaticFinding entity = new StaticFinding();
        entity.setTaskId(finding.getTaskId());
        entity.setRuleId(finding.getRuleId());
        entity.setSeverity(finding.getSeverity());
        entity.setConfidence(finding.getConfidence());
        entity.setSourceFile(finding.getSourceFile());
        entity.setLineNumber(finding.getLineNumber());
        entity.setLoopStartLine(finding.getLoopStartLine());
        entity.setLoopEndLine(finding.getLoopEndLine());
        entity.setLoopMethodName(finding.getLoopMethodName());
        entity.setIoType(finding.getIoType());
        entity.setRiskScope(finding.getRiskScope());
        entity.setChangedLine(finding.isChangedLine());
        entity.setIntroducedByName(finding.getIntroducedByName());
        entity.setIntroducedByEmail(finding.getIntroducedByEmail());
        entity.setIntroducedCommit(finding.getIntroducedCommit());
        entity.setIntroducedCommitTime(finding.getIntroducedCommitTime());
        entity.setEvidenceHash(finding.getEvidenceHash());
        entity.setRawPayload(finding.getRawPayload());
        return entity;
    }

    private StaticFindingBO toStaticFindingBO(StaticFinding entity) {
        StaticFindingBO finding = new StaticFindingBO();
        finding.setId(entity.getId());
        finding.setTaskId(entity.getTaskId());
        finding.setRuleId(entity.getRuleId());
        finding.setSeverity(entity.getSeverity());
        finding.setConfidence(entity.getConfidence());
        finding.setSourceFile(entity.getSourceFile());
        finding.setLineNumber(value(entity.getLineNumber()));
        finding.setLoopStartLine(value(entity.getLoopStartLine()));
        finding.setLoopEndLine(value(entity.getLoopEndLine()));
        finding.setLoopMethodName(entity.getLoopMethodName());
        finding.setIoType(entity.getIoType());
        finding.setRiskScope(entity.getRiskScope());
        finding.setChangedLine(Boolean.TRUE.equals(entity.getChangedLine()));
        finding.setIntroducedByName(entity.getIntroducedByName());
        finding.setIntroducedByEmail(entity.getIntroducedByEmail());
        finding.setIntroducedCommit(entity.getIntroducedCommit());
        finding.setIntroducedCommitTime(entity.getIntroducedCommitTime());
        finding.setEvidenceHash(entity.getEvidenceHash());
        finding.setRawPayload(entity.getRawPayload());
        return finding;
    }

    private DynamicEvidenceBO toDynamicEvidenceBO(DynamicEvidence entity) {
        DynamicEvidenceBO evidence = new DynamicEvidenceBO();
        evidence.setId(entity.getId());
        evidence.setTaskId(entity.getTaskId());
        evidence.setEnv(entity.getEnvName());
        evidence.setAppName(entity.getAppName());
        evidence.setEntryKey(entity.getEntryKey());
        evidence.setRawPayload(entity.getRawPayload());
        return evidence;
    }

    private StaticDynamicCorroboration toStaticDynamicCorroboration(StaticDynamicCorroborationBO corroboration) {
        StaticDynamicCorroboration entity = new StaticDynamicCorroboration();
        entity.setTaskId(corroboration.getTaskId());
        entity.setStaticFindingId(corroboration.getStaticFindingId());
        entity.setFindingIssueId(corroboration.getFindingIssueId());
        entity.setStatus(corroboration.getStatus());
        entity.setHitRequestCount(corroboration.getHitRequestCount());
        entity.setHitEntryCount(corroboration.getHitEntryCount());
        entity.setMaxRepeatCount(corroboration.getMaxRepeatCount());
        entity.setAvgRepeatCount(corroboration.getAvgRepeatCount());
        entity.setTopEntryKey(corroboration.getTopEntryKey());
        entity.setReason(corroboration.getReason());
        return entity;
    }

    private StaticDynamicCorroborationBO toStaticDynamicCorroborationBO(StaticDynamicCorroboration entity) {
        StaticDynamicCorroborationBO corroboration = new StaticDynamicCorroborationBO();
        corroboration.setId(entity.getId());
        corroboration.setTaskId(entity.getTaskId());
        corroboration.setStaticFindingId(entity.getStaticFindingId());
        corroboration.setFindingIssueId(entity.getFindingIssueId());
        corroboration.setStatus(entity.getStatus());
        corroboration.setHitRequestCount(value(entity.getHitRequestCount()));
        corroboration.setHitEntryCount(value(entity.getHitEntryCount()));
        corroboration.setMaxRepeatCount(value(entity.getMaxRepeatCount()));
        corroboration.setAvgRepeatCount(value(entity.getAvgRepeatCount()));
        corroboration.setTopEntryKey(entity.getTopEntryKey());
        corroboration.setReason(entity.getReason());
        return corroboration;
    }

    private DynamicRequestEvidence toDynamicRequestEvidence(DynamicRequestEvidenceBO evidence) {
        DynamicRequestEvidence entity = new DynamicRequestEvidence();
        entity.setTaskId(evidence.getTaskId());
        entity.setRawEvidenceId(evidence.getRawEvidenceId());
        entity.setEnvName(evidence.getEnv());
        entity.setAppName(evidence.getAppName());
        entity.setEntryMethod(evidence.getEntryMethod());
        entity.setEntryPath(evidence.getEntryPath());
        entity.setEntryKey(evidence.getEntryKey());
        entity.setWallTimeMs(evidence.getWallTimeMs());
        entity.setRawPayload(evidence.getRawPayload());
        return entity;
    }

    private DynamicRequestEvidenceBO toDynamicRequestEvidenceBO(DynamicRequestEvidence entity) {
        DynamicRequestEvidenceBO evidence = new DynamicRequestEvidenceBO();
        evidence.setId(entity.getId());
        evidence.setTaskId(entity.getTaskId());
        evidence.setRawEvidenceId(entity.getRawEvidenceId());
        evidence.setEnv(entity.getEnvName());
        evidence.setAppName(entity.getAppName());
        evidence.setEntryMethod(entity.getEntryMethod());
        evidence.setEntryPath(entity.getEntryPath());
        evidence.setEntryKey(entity.getEntryKey());
        evidence.setWallTimeMs(value(entity.getWallTimeMs()));
        evidence.setRawPayload(entity.getRawPayload());
        return evidence;
    }

    private DynamicCallEvidence toDynamicCallEvidence(DynamicCallEvidenceBO evidence) {
        DynamicCallEvidence entity = new DynamicCallEvidence();
        entity.setTaskId(evidence.getTaskId());
        entity.setRequestEvidenceId(evidence.getRequestEvidenceId());
        entity.setRawEvidenceId(evidence.getRawEvidenceId());
        entity.setEntryKey(evidence.getEntryKey());
        entity.setClassName(evidence.getClassName());
        entity.setMethodName(evidence.getMethodName());
        entity.setFullMethodName(evidence.getFullMethodName());
        entity.setCallPath(evidence.getCallPath());
        entity.setCallCount(evidence.getCallCount());
        entity.setTotalTimeMs(evidence.getTotalTimeMs());
        entity.setRawPayload(evidence.getRawPayload());
        return entity;
    }

    private DynamicCallEvidenceBO toDynamicCallEvidenceBO(DynamicCallEvidence entity) {
        DynamicCallEvidenceBO evidence = new DynamicCallEvidenceBO();
        evidence.setId(entity.getId());
        evidence.setTaskId(entity.getTaskId());
        evidence.setRequestEvidenceId(entity.getRequestEvidenceId());
        evidence.setRawEvidenceId(entity.getRawEvidenceId());
        evidence.setEntryKey(entity.getEntryKey());
        evidence.setClassName(entity.getClassName());
        evidence.setMethodName(entity.getMethodName());
        evidence.setFullMethodName(entity.getFullMethodName());
        evidence.setCallPath(entity.getCallPath());
        evidence.setCallCount(value(entity.getCallCount()));
        evidence.setTotalTimeMs(value(entity.getTotalTimeMs()));
        evidence.setRawPayload(entity.getRawPayload());
        return evidence;
    }

    private FindingIssueBO toFindingIssueBO(FindingIssue entity) {
        FindingIssueBO issue = new FindingIssueBO();
        issue.setId(entity.getId());
        issue.setIssueKey(entity.getIssueKey());
        issue.setRepositoryId(entity.getRepositoryId());
        issue.setBranchName(entity.getBranchName());
        issue.setRuleId(entity.getRuleId());
        issue.setEvidenceHash(entity.getEvidenceHash());
        issue.setSourceFile(entity.getSourceFile());
        issue.setLineNumber(value(entity.getLineNumber()));
        issue.setLoopMethodName(entity.getLoopMethodName());
        issue.setIoType(entity.getIoType());
        issue.setOwnerName(entity.getOwnerName());
        issue.setOwnerEmail(entity.getOwnerEmail());
        issue.setStatus(entity.getStatus());
        issue.setSeverity(entity.getSeverity());
        issue.setFirstSeenTaskId(entity.getFirstSeenTaskId());
        issue.setLastSeenTaskId(entity.getLastSeenTaskId());
        issue.setRawPayload(entity.getRawPayload());
        return issue;
    }

    private AnalysisTaskBO toDomain(AnalysisTask entity) {
        CodeRepository repository = repositoryMapper.selectById(entity.getRepositoryId());
        GitCommit gitCommit = gitCommitMapper.selectById(entity.getGitCommitId());
        AnalysisTaskBO task = new AnalysisTaskBO(
                entity.getTaskId(),
                repository == null ? "" : repository.getProjectName(),
                repository == null ? "" : repository.getRemoteUrl(),
                gitCommit == null ? "" : gitCommit.getCommitSha(),
                gitCommit == null ? "" : gitCommit.getBranchName(),
                entity.getEnvName(),
                gitCommit == null ? "" : gitCommit.getAuthorName(),
                gitCommit == null ? "" : gitCommit.getAuthorEmail(),
                gitCommit == null ? "" : formatDateTime(gitCommit.getAuthorTime()),
                gitCommit == null ? "" : gitCommit.getCommitterName(),
                gitCommit == null ? "" : gitCommit.getCommitterEmail(),
                gitCommit == null ? "" : gitCommit.getCommitMessage());
        task.setStatus(TaskStatus.valueOf(entity.getStatus()));
        task.setRiskLevel(RiskLevel.valueOf(entity.getRiskLevel()));
        task.setStaticRiskLevel(RiskLevel.valueOf(entity.getStaticRiskLevel()));
        task.setStaticPayload(entity.getStaticPayload());
        task.setDynamicPayload(entity.getDynamicPayload());
        task.setCreatedAt(formatDateTime(entity.getCreatedAt()));
        task.setUpdatedAt(formatDateTime(entity.getUpdatedAt()));
        return task;
    }

    private CodeRepository saveRepository(AnalysisTaskBO task) {
        String repoKey = repoKey(task);
        CodeRepository existing = findRepository(repoKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        CodeRepository entity = new CodeRepository();
        entity.setRepoKey(repoKey);
        entity.setProjectName(task.getProject());
        entity.setRemoteUrl(task.getRemoteUrl());
        entity.setProvider(provider(task.getRemoteUrl()));
        entity.setNamespace(namespace(task.getRemoteUrl()));
        entity.setRepoName(repoName(task.getRemoteUrl(), task.getProject()));
        entity.setDefaultBranch(task.getBranch());
        repositoryMapper.insert(entity);
        return entity;
    }

    private Optional<CodeRepository> findRepository(String repoKey) {
        LambdaQueryWrapper<CodeRepository> query = new LambdaQueryWrapper<>();
        query.eq(CodeRepository::getRepoKey, repoKey);
        query.last("LIMIT 1");
        return Optional.ofNullable(repositoryMapper.selectOne(query));
    }

    private GitCommit saveGitCommit(AnalysisTaskBO task, CodeRepository repository) {
        GitCommit existing = findGitCommit(repository.getId(), task.getCommit(), task.getBranch()).orElse(null);
        if (existing != null) {
            mergeGitCommitMetadata(existing, task);
            gitCommitMapper.updateById(existing);
            return existing;
        }
        GitCommit entity = new GitCommit();
        entity.setRepositoryId(repository.getId());
        entity.setCommitSha(task.getCommit());
        entity.setBranchName(task.getBranch());
        entity.setAuthorName(task.getAuthorName());
        entity.setAuthorEmail(task.getAuthorEmail());
        entity.setAuthorTime(parseAuthorTime(task.getAuthorTime()));
        entity.setCommitterName(task.getCommitterName());
        entity.setCommitterEmail(task.getCommitterEmail());
        entity.setCommitMessage(task.getCommitMessage());
        entity.setRemoteUrlSnapshot(task.getRemoteUrl());
        gitCommitMapper.insert(entity);
        return entity;
    }

    private void mergeGitCommitMetadata(GitCommit entity, AnalysisTaskBO task) {
        entity.setAuthorName(firstNonBlank(task.getAuthorName(), entity.getAuthorName()));
        entity.setAuthorEmail(firstNonBlank(task.getAuthorEmail(), entity.getAuthorEmail()));
        LocalDateTime parsedAuthorTime = parseAuthorTime(task.getAuthorTime());
        if (parsedAuthorTime != null) {
            entity.setAuthorTime(parsedAuthorTime);
        }
        entity.setCommitterName(firstNonBlank(task.getCommitterName(), entity.getCommitterName()));
        entity.setCommitterEmail(firstNonBlank(task.getCommitterEmail(), entity.getCommitterEmail()));
        entity.setCommitMessage(firstNonBlank(task.getCommitMessage(), entity.getCommitMessage()));
        entity.setRemoteUrlSnapshot(firstNonBlank(task.getRemoteUrl(), entity.getRemoteUrlSnapshot()));
    }

    private Optional<GitCommit> findGitCommit(Long repositoryId, String commit, String branch) {
        LambdaQueryWrapper<GitCommit> query = new LambdaQueryWrapper<>();
        query.eq(GitCommit::getRepositoryId, repositoryId);
        query.eq(GitCommit::getCommitSha, commit);
        query.eq(GitCommit::getBranchName, branch);
        query.last("LIMIT 1");
        return Optional.ofNullable(gitCommitMapper.selectOne(query));
    }

    private String repoKey(AnalysisTaskBO task) {
        String remoteUrl = task.getRemoteUrl();
        if (remoteUrl != null && !remoteUrl.trim().isEmpty() && !"UNKNOWN".equals(remoteUrl)) {
            return remoteUrl.trim().toLowerCase();
        }
        return ("project:" + task.getProject()).toLowerCase();
    }

    private String repoKey(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty() || "UNKNOWN".equals(remoteUrl)) {
            return "";
        }
        return remoteUrl.trim().toLowerCase();
    }

    private String provider(String remoteUrl) {
        if (remoteUrl == null) {
            return "";
        }
        String value = remoteUrl.toLowerCase();
        if (value.contains("gitlab")) {
            return "gitlab";
        }
        if (value.contains("github")) {
            return "github";
        }
        if (value.contains("gitee")) {
            return "gitee";
        }
        return "";
    }

    private String namespace(String remoteUrl) {
        String normalized = normalizeRemote(remoteUrl);
        int slash = normalized.lastIndexOf('/');
        if (slash <= 0) {
            return "";
        }
        int previous = normalized.lastIndexOf('/', slash - 1);
        return previous >= 0 ? normalized.substring(previous + 1, slash) : "";
    }

    private String repoName(String remoteUrl, String fallback) {
        String normalized = normalizeRemote(remoteUrl);
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }
        return name.trim().isEmpty() ? fallback : name;
    }

    private String normalizeRemote(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            return "";
        }
        return remoteUrl.trim().replace(':', '/').replace('\\', '/');
    }

    private LocalDateTime parseAuthorTime(String authorTime) {
        if (authorTime == null || authorTime.trim().isEmpty()) {
            return null;
        }
        String value = authorTime.trim();
        if (value.matches("\\d+")) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(value)), ZoneId.systemDefault());
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.debug("无法使用 OffsetDateTime 格式解析时间: {}", value);
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            log.debug("无法解析作者时间，所有格式均不匹配: {}", value);
            return null;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }

    private boolean same(String left, String right) {
        return firstNonBlank(left, "").trim().equals(firstNonBlank(right, "").trim());
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
