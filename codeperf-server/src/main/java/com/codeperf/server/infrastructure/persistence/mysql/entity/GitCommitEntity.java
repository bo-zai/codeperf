package com.codeperf.server.infrastructure.persistence.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("git_commit")
public class GitCommitEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("repository_id")
    private Long repositoryId;

    @TableField("commit_sha")
    private String commitSha;

    @TableField("branch_name")
    private String branchName;

    @TableField("author_name")
    private String authorName;

    @TableField("author_email")
    private String authorEmail;

    @TableField("author_time")
    private String authorTime;

    @TableField("committer_name")
    private String committerName;

    @TableField("committer_email")
    private String committerEmail;

    @TableField("commit_message")
    private String commitMessage;

    @TableField("remote_url_snapshot")
    private String remoteUrlSnapshot;
}
