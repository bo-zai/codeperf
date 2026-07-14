package com.codeperf.server.infrastructure.persistence.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dynamic_evidence")
public class DynamicEvidenceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    @TableField("repository_id")
    private Long repositoryId;

    @TableField("git_commit_id")
    private Long gitCommitId;

    @TableField("env_name")
    private String envName;

    @TableField("app_name")
    private String appName;

    @TableField("entry_key")
    private String entryKey;

    @TableField("raw_payload")
    private String rawPayload;
}
