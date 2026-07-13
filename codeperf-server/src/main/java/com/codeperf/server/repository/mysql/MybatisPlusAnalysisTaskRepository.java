package com.codeperf.server.repository.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codeperf.server.model.AnalysisTask;
import com.codeperf.server.model.RiskLevel;
import com.codeperf.server.model.TaskStatus;
import com.codeperf.server.repository.AnalysisTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "codeperf.storage.mode", havingValue = "mysql")
public class MybatisPlusAnalysisTaskRepository implements AnalysisTaskRepository {

    private final AnalysisTaskMapper mapper;

    public MybatisPlusAnalysisTaskRepository(AnalysisTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AnalysisTask save(AnalysisTask task) {
        AnalysisTaskEntity entity = toEntity(task);
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

    private Optional<AnalysisTaskEntity> findEntity(String taskId) {
        LambdaQueryWrapper<AnalysisTaskEntity> query = new LambdaQueryWrapper<>();
        query.eq(AnalysisTaskEntity::getTaskId, taskId);
        query.last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(query));
    }

    private AnalysisTaskEntity toEntity(AnalysisTask task) {
        AnalysisTaskEntity entity = new AnalysisTaskEntity();
        entity.setTaskId(task.getAnalysisTaskId());
        entity.setProject(task.getProject());
        entity.setBranchName(task.getBranch());
        entity.setCommitSha(task.getCommit());
        entity.setEnvName(task.getEnv());
        entity.setStatus(task.getStatus().name());
        entity.setRiskLevel(task.getRiskLevel().name());
        entity.setStaticRiskLevel(task.getStaticRiskLevel().name());
        entity.setStaticPayload(task.getStaticPayload());
        entity.setDynamicPayload(task.getDynamicPayload());
        return entity;
    }

    private AnalysisTask toDomain(AnalysisTaskEntity entity) {
        AnalysisTask task = new AnalysisTask(
                entity.getTaskId(),
                entity.getProject(),
                entity.getCommitSha(),
                entity.getBranchName(),
                entity.getEnvName());
        task.setStatus(TaskStatus.valueOf(entity.getStatus()));
        task.setRiskLevel(RiskLevel.valueOf(entity.getRiskLevel()));
        task.setStaticRiskLevel(RiskLevel.valueOf(entity.getStaticRiskLevel()));
        task.setStaticPayload(entity.getStaticPayload());
        task.setDynamicPayload(entity.getDynamicPayload());
        return task;
    }
}
