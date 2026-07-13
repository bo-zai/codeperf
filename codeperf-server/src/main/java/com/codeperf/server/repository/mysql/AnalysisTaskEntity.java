package com.codeperf.server.repository.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("analysis_task")
public class AnalysisTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    private String project;

    @TableField("branch_name")
    private String branchName;

    @TableField("commit_sha")
    private String commitSha;

    @TableField("env_name")
    private String envName;

    private String status;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("static_risk_level")
    private String staticRiskLevel;

    @TableField("static_payload")
    private String staticPayload;

    @TableField("dynamic_payload")
    private String dynamicPayload;
}
