package com.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("git_commit")
public class GitCommit {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 代码仓库ID，逻辑关联code_repository.id */
    @TableField("repository_id")
    private Long repositoryId;

    /** Git提交SHA */
    @TableField("commit_sha")
    private String commitSha;

    /** 分支名称 */
    @TableField("branch_name")
    private String branchName;

    /** 提交作者姓名 */
    @TableField("author_name")
    private String authorName;

    /** 提交作者邮箱 */
    @TableField("author_email")
    private String authorEmail;

    /** 提交作者时间 */
    @TableField("author_time")
    private LocalDateTime authorTime;

    /** 提交执行人姓名 */
    @TableField("committer_name")
    private String committerName;

    /** 提交执行人邮箱 */
    @TableField("committer_email")
    private String committerEmail;

    /** 提交说明 */
    @TableField("commit_message")
    private String commitMessage;

    /** 提交时采集到的远程仓库地址快照 */
    @TableField("remote_url_snapshot")
    private String remoteUrlSnapshot;
}
