package com.codeperf.server.service.repository.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.model.bo.DynamicEvidenceBO;
import com.codeperf.server.model.bo.RiskLevel;
import com.codeperf.server.model.bo.StaticFindingBO;
import com.codeperf.server.model.bo.TaskStatus;
import com.codeperf.server.service.repository.AnalysisTaskRepository;
import com.codeperf.server.model.entity.AnalysisTask;
import com.codeperf.server.model.entity.CodeRepository;
import com.codeperf.server.model.entity.DynamicEvidence;
import com.codeperf.server.model.entity.GitCommit;
import com.codeperf.server.model.entity.RuleDefinition;
import com.codeperf.server.model.entity.StaticFinding;
import com.codeperf.server.mapper.AnalysisTaskMapper;
import com.codeperf.server.mapper.CodeRepositoryMapper;
import com.codeperf.server.mapper.DynamicEvidenceMapper;
import com.codeperf.server.mapper.GitCommitMapper;
import com.codeperf.server.mapper.RuleDefinitionMapper;
import com.codeperf.server.mapper.StaticFindingMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "codeperf.storage.mode", havingValue = "mysql")
public class MybatisPlusAnalysisTaskRepository implements AnalysisTaskRepository {

    private final AnalysisTaskMapper mapper;
    private final CodeRepositoryMapper repositoryMapper;
    private final GitCommitMapper gitCommitMapper;
    private final StaticFindingMapper staticFindingMapper;
    private final DynamicEvidenceMapper dynamicEvidenceMapper;
    private final RuleDefinitionMapper ruleDefinitionMapper;

    public MybatisPlusAnalysisTaskRepository(AnalysisTaskMapper mapper,
                                             CodeRepositoryMapper repositoryMapper,
                                             GitCommitMapper gitCommitMapper,
                                             StaticFindingMapper staticFindingMapper,
                                             DynamicEvidenceMapper dynamicEvidenceMapper,
                                             RuleDefinitionMapper ruleDefinitionMapper) {
        this.mapper = mapper;
        this.repositoryMapper = repositoryMapper;
        this.gitCommitMapper = gitCommitMapper;
        this.staticFindingMapper = staticFindingMapper;
        this.dynamicEvidenceMapper = dynamicEvidenceMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
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
    public Optional<AnalysisTaskBO> findByCommitIdentity(String remoteUrl, String commit, String branch, String env) {
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
        query.last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query)).map(this::toDomain);
    }

    @Override
    public void replaceStaticFindings(String taskId, List<StaticFindingBO> findings) {
        LambdaQueryWrapper<StaticFinding> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(StaticFinding::getTaskId, taskId);
        staticFindingMapper.delete(deleteQuery);
        for (StaticFindingBO finding : findings) {
            staticFindingMapper.insert(toStaticFinding(finding));
        }
    }

    @Override
    public void appendDynamicEvidence(DynamicEvidenceBO evidence) {
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
    }

    @Override
    public boolean isRuleDefined(String ruleId) {
        LambdaQueryWrapper<RuleDefinition> query = new LambdaQueryWrapper<>();
        query.eq(RuleDefinition::getRuleId, ruleId);
        query.eq(RuleDefinition::getEnabled, Boolean.TRUE);
        query.last("LIMIT 1");
        return ruleDefinitionMapper.selectOne(query) != null;
    }

    private Optional<AnalysisTask> findEntity(String taskId) {
        LambdaQueryWrapper<AnalysisTask> query = new LambdaQueryWrapper<>();
        query.eq(AnalysisTask::getTaskId, taskId);
        query.last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query));
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
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.toString();
    }
}
