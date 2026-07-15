package com.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("analysis_task")
public class AnalysisTask {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分析任务唯一ID */
    @TableField("task_id")
    private String taskId;

    /** 代码仓库ID，逻辑关联code_repository.id */
    @TableField("repository_id")
    private Long repositoryId;

    /** Git提交ID，逻辑关联git_commit.id */
    @TableField("git_commit_id")
    private Long gitCommitId;

    /** 环境名称，例如local、dev、preprod */
    @TableField("env_name")
    private String envName;

    /** 任务状态 */
    private String status;

    /** 综合风险等级 */
    @TableField("risk_level")
    private String riskLevel;

    /** 静态扫描风险等级 */
    @TableField("static_risk_level")
    private String staticRiskLevel;

    /** 静态扫描原始报告JSON */
    @TableField("static_payload")
    private String staticPayload;

    /** 动态证据原始报告JSON */
    @TableField("dynamic_payload")
    private String dynamicPayload;
}
