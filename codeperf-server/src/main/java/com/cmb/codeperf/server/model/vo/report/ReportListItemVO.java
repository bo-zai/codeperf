package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

/**
 * 报告列表中的单个任务摘要。
 */
@Data
public class ReportListItemVO {

    /** 分析任务ID */
    private String taskId;

    /** 项目名称 */
    private String projectName;

    /** Git分支 */
    private String branch;

    /** Git提交SHA */
    private String commit;

    /** 运行环境 */
    private String env;

    /** 任务生成时间 */
    private String createdAt;

    /** 任务更新时间 */
    private String updatedAt;

    /** 任务状态 */
    private String status;

    /** 综合风险等级 */
    private String riskLevel;

    /** 静态风险等级 */
    private String staticRiskLevel;

    /** 是否已有静态扫描结果 */
    private boolean hasStaticResult;

    /** 是否已有动态运行证据 */
    private boolean hasDynamicEvidence;

    /** 提交作者姓名 */
    private String authorName;

    /** 提交作者邮箱 */
    private String authorEmail;

    /** 提交说明 */
    private String commitMessage;
}
