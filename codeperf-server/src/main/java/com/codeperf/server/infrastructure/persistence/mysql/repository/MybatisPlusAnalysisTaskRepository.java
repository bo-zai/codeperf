package com.codeperf.server.infrastructure.persistence.mysql.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeperf.server.domain.model.AnalysisTask;
import com.codeperf.server.domain.model.DynamicEvidenceRecord;
import com.codeperf.server.domain.model.RiskLevel;
import com.codeperf.server.domain.model.StaticFindingRecord;
import com.codeperf.server.domain.model.TaskStatus;
import com.codeperf.server.domain.repository.AnalysisTaskRepository;
import com.codeperf.server.infrastructure.persistence.mysql.entity.AnalysisTaskEntity;
import com.codeperf.server.infrastructure.persistence.mysql.entity.CodeRepositoryEntity;
import com.codeperf.server.infrastructure.persistence.mysql.entity.DynamicEvidenceEntity;
import com.codeperf.server.infrastructure.persistence.mysql.entity.GitCommitEntity;
import com.codeperf.server.infrastructure.persistence.mysql.entity.StaticFindingEntity;
import com.codeperf.server.infrastructure.persistence.mysql.mapper.AnalysisTaskMapper;
import com.codeperf.server.infrastructure.persistence.mysql.mapper.CodeRepositoryMapper;
import com.codeperf.server.infrastructure.persistence.mysql.mapper.DynamicEvidenceMapper;
import com.codeperf.server.infrastructure.persistence.mysql.mapper.GitCommitMapper;
import com.codeperf.server.infrastructure.persistence.mysql.mapper.StaticFindingMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

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

    public MybatisPlusAnalysisTaskRepository(AnalysisTaskMapper mapper,
                                             CodeRepositoryMapper repositoryMapper,
                                             GitCommitMapper gitCommitMapper,
                                             StaticFindingMapper staticFindingMapper,
                                             DynamicEvidenceMapper dynamicEvidenceMapper) {
        this.mapper = mapper;
        this.repositoryMapper = repositoryMapper;
        this.gitCommitMapper = gitCommitMapper;
        this.staticFindingMapper = staticFindingMapper;
        this.dynamicEvidenceMapper = dynamicEvidenceMapper;
    }

    @Override
    public AnalysisTask save(AnalysisTask task) {
        CodeRepositoryEntity repository = saveRepository(task);
        GitCommitEntity gitCommit = saveGitCommit(task, repository);
        AnalysisTaskEntity entity = toEntity(task, repository, gitCommit);
        AnalysisTaskEntity existing = findEntity(task.getAnalysisTaskId()).orElse(null);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            mapper.updateById(entity);
        }
        return task;
    }

    @Override
    public Optional<AnalysisTask> findByTaskId(String taskId) {
        return findEntity(taskId).map(this::toDomain);
    }

    @Override
    public void replaceStaticFindings(String taskId, List<StaticFindingRecord> findings) {
        LambdaQueryWrapper<StaticFindingEntity> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(StaticFindingEntity::getTaskId, taskId);
        staticFindingMapper.delete(deleteQuery);
        for (StaticFindingRecord finding : findings) {
            staticFindingMapper.insert(toStaticFindingEntity(finding));
        }
    }

    @Override
    public void appendDynamicEvidence(DynamicEvidenceRecord evidence) {
        DynamicEvidenceEntity entity = new DynamicEvidenceEntity();
        entity.setTaskId(evidence.getTaskId());
        entity.setEnvName(evidence.getEnv());
        entity.setAppName(evidence.getAppName());
        entity.setEntryKey(evidence.getEntryKey());
        entity.setRawPayload(evidence.getRawPayload());
        findByTaskId(evidence.getTaskId()).ifPresent(task -> {
            CodeRepositoryEntity repository = saveRepository(task);
            GitCommitEntity gitCommit = saveGitCommit(task, repository);
            entity.setRepositoryId(repository.getId());
            entity.setGitCommitId(gitCommit.getId());
        });
        dynamicEvidenceMapper.insert(entity);
    }

    private Optional<AnalysisTaskEntity> findEntity(String taskId) {
        LambdaQueryWrapper<AnalysisTaskEntity> query = new LambdaQueryWrapper<>();
        query.eq(AnalysisTaskEntity::getTaskId, taskId);
        query.last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query));
    }

    private AnalysisTaskEntity toEntity(AnalysisTask task, CodeRepositoryEntity repository, GitCommitEntity gitCommit) {
        AnalysisTaskEntity entity = new AnalysisTaskEntity();
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

    private StaticFindingEntity toStaticFindingEntity(StaticFindingRecord finding) {
        StaticFindingEntity entity = new StaticFindingEntity();
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

    private AnalysisTask toDomain(AnalysisTaskEntity entity) {
        CodeRepositoryEntity repository = repositoryMapper.selectById(entity.getRepositoryId());
        GitCommitEntity gitCommit = gitCommitMapper.selectById(entity.getGitCommitId());
        AnalysisTask task = new AnalysisTask(
                entity.getTaskId(),
                repository == null ? "" : repository.getProjectName(),
                repository == null ? "" : repository.getRemoteUrl(),
                gitCommit == null ? "" : gitCommit.getCommitSha(),
                gitCommit == null ? "" : gitCommit.getBranchName(),
                entity.getEnvName(),
                gitCommit == null ? "" : gitCommit.getAuthorName(),
                gitCommit == null ? "" : gitCommit.getAuthorEmail(),
                gitCommit == null ? "" : gitCommit.getAuthorTime(),
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

    private CodeRepositoryEntity saveRepository(AnalysisTask task) {
        String repoKey = repoKey(task);
        CodeRepositoryEntity existing = findRepository(repoKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        CodeRepositoryEntity entity = new CodeRepositoryEntity();
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

    private Optional<CodeRepositoryEntity> findRepository(String repoKey) {
        LambdaQueryWrapper<CodeRepositoryEntity> query = new LambdaQueryWrapper<>();
        query.eq(CodeRepositoryEntity::getRepoKey, repoKey);
        query.last("LIMIT 1");
        return Optional.ofNullable(repositoryMapper.selectOne(query));
    }

    private GitCommitEntity saveGitCommit(AnalysisTask task, CodeRepositoryEntity repository) {
        GitCommitEntity existing = findGitCommit(repository.getId(), task.getCommit(), task.getBranch()).orElse(null);
        if (existing != null) {
            return existing;
        }
        GitCommitEntity entity = new GitCommitEntity();
        entity.setRepositoryId(repository.getId());
        entity.setCommitSha(task.getCommit());
        entity.setBranchName(task.getBranch());
        entity.setAuthorName(task.getAuthorName());
        entity.setAuthorEmail(task.getAuthorEmail());
        entity.setAuthorTime(task.getAuthorTime());
        entity.setCommitterName(task.getCommitterName());
        entity.setCommitterEmail(task.getCommitterEmail());
        entity.setCommitMessage(task.getCommitMessage());
        entity.setRemoteUrlSnapshot(task.getRemoteUrl());
        gitCommitMapper.insert(entity);
        return entity;
    }

    private Optional<GitCommitEntity> findGitCommit(Long repositoryId, String commit, String branch) {
        LambdaQueryWrapper<GitCommitEntity> query = new LambdaQueryWrapper<>();
        query.eq(GitCommitEntity::getRepositoryId, repositoryId);
        query.eq(GitCommitEntity::getCommitSha, commit);
        query.eq(GitCommitEntity::getBranchName, branch);
        query.last("LIMIT 1");
        return Optional.ofNullable(gitCommitMapper.selectOne(query));
    }

    private String repoKey(AnalysisTask task) {
        String remoteUrl = task.getRemoteUrl();
        if (remoteUrl != null && !remoteUrl.trim().isEmpty() && !"UNKNOWN".equals(remoteUrl)) {
            return remoteUrl.trim().toLowerCase();
        }
        return ("project:" + task.getProject()).toLowerCase();
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
}
