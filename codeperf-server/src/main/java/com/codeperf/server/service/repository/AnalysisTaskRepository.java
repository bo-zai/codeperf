package com.codeperf.server.service.repository;

import com.codeperf.server.model.bo.AnalysisTaskBO;
import com.codeperf.server.model.bo.DynamicEvidenceBO;
import com.codeperf.server.model.bo.StaticFindingBO;

import java.util.List;
import java.util.Optional;

/**
 * 分析任务仓储接口。
 * 服务层只依赖该接口，避免业务逻辑绑定到内存 Map 或具体 ORM 实现。
 */
public interface AnalysisTaskRepository {

    /**
     * 保存或更新分析任务。
     *
     * @param task 分析任务
     * @return 保存后的任务
     */
    AnalysisTaskBO save(AnalysisTaskBO task);

    /**
     * 按任务 ID 查询分析任务。
     *
     * @param taskId 分析任务 ID
     * @return 查询结果
     */
    Optional<AnalysisTaskBO> findByTaskId(String taskId);

    Optional<AnalysisTaskBO> findByCommitIdentity(String remoteUrl, String commit, String branch, String env);

    void replaceStaticFindings(String taskId, List<StaticFindingBO> findings);

    void appendDynamicEvidence(DynamicEvidenceBO evidence);

    boolean isRuleDefined(String ruleId);
}
