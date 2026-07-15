package com.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dynamic_evidence")
public class DynamicEvidence {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分析任务ID，逻辑关联analysis_task.task_id */
    @TableField("task_id")
    private String taskId;

    /** Agent会话ID，逻辑关联agent_session.id */
    @TableField("agent_session_id")
    private Long agentSessionId;

    /** 代码仓库ID，逻辑关联code_repository.id */
    @TableField("repository_id")
    private Long repositoryId;

    /** Git提交ID，逻辑关联git_commit.id */
    @TableField("git_commit_id")
    private Long gitCommitId;

    /** 环境名称 */
    @TableField("env_name")
    private String envName;

    /** 应用名称 */
    @TableField("app_name")
    private String appName;

    /** 入口方法或接口标识 */
    @TableField("entry_key")
    private String entryKey;

    /** 动态证据原始JSON */
    @TableField("raw_payload")
    private String rawPayload;
}
