package com.cmb.codeperf.server.service.repository;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.DynamicCallEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.DynamicRequestEvidenceBO;
import com.cmb.codeperf.server.model.bo.FindingIssueBO;
import com.cmb.codeperf.server.model.bo.FindingOccurrenceBO;
import com.cmb.codeperf.server.model.bo.StaticDynamicCorroborationBO;
import com.cmb.codeperf.server.model.bo.StaticFindingBO;

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

    /**
     * 按 Git 构建身份查询唯一分析任务。
     * 静态扫描与动态证据通过同一仓库、提交、分支、环境形成稳定关联，重复扫描应复用同一个任务。
     *
     * @param remoteUrl 远程仓库地址
     * @param commit 提交 SHA
     * @param branch 分支名称
     * @param env 环境名称
     * @return 匹配任务
     */
    Optional<AnalysisTaskBO> findByCommitIdentity(String remoteUrl, String commit, String branch, String env);

    /**
     * 查询最近创建的分析任务。
     *
     * @param limit 返回数量上限
     * @return 最近任务列表，按创建顺序倒序
     */
    /**
     * 查询最近有风险的分析任务。
     *
     * @param limit 返回数量上限
     * @return 最近风险任务列表，按创建顺序倒序
     */
    List<AnalysisTaskBO> listRecentRiskTasks(int limit);

    /**
     * 查询任务下的静态风险明细。
     *
     * @param taskId 分析任务 ID
     * @return 静态风险列表
     */
    List<StaticFindingBO> listStaticFindings(String taskId);

    /**
     * 查询任务下的动态运行证据。
     *
     * @param taskId 分析任务 ID
     * @return 动态证据列表
     */
    List<DynamicEvidenceBO> listDynamicEvidence(String taskId);

    void replaceStaticFindings(String taskId, List<StaticFindingBO> findings);

    DynamicEvidenceBO appendDynamicEvidence(DynamicEvidenceBO evidence);

    void appendDynamicRequestEvidence(DynamicRequestEvidenceBO evidence);

    void appendDynamicCallEvidence(DynamicCallEvidenceBO evidence);

    List<DynamicRequestEvidenceBO> listDynamicRequestEvidence(String taskId);

    List<DynamicCallEvidenceBO> listDynamicCallEvidence(String taskId);

    void replaceStaticDynamicCorroborations(String taskId, List<StaticDynamicCorroborationBO> corroborations);

    List<StaticDynamicCorroborationBO> listStaticDynamicCorroborations(String taskId);

    boolean isRuleDefined(String ruleId);

    FindingIssueBO saveOrUpdateIssue(AnalysisTaskBO task, StaticFindingBO finding, String issueKey);

    Optional<FindingIssueBO> findIssueByIssueKey(String issueKey);

    void appendFindingOccurrence(FindingOccurrenceBO occurrence);

    List<FindingIssueBO> listOpenIssues(String remoteUrl, String branch);

    List<DynamicEvidenceBO> listLatestDynamicEvidence(String remoteUrl, String branch, String env);

    void closeResolvedIssues(AnalysisTaskBO task, List<String> scannedSourceFiles, List<String> currentIssueKeys);
}

