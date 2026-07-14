package com.codeperf.server.infrastructure.persistence.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("code_repository")
public class CodeRepositoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("repo_key")
    private String repoKey;

    @TableField("project_name")
    private String projectName;

    @TableField("remote_url")
    private String remoteUrl;

    private String provider;

    private String namespace;

    @TableField("repo_name")
    private String repoName;

    @TableField("default_branch")
    private String defaultBranch;
}
