package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("code_repository")
public class CodeRepository {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 仓库唯一标识，优先使用归一化后的远程仓库地址 */
    @TableField("repo_key")
    private String repoKey;

    /** 项目名称 */
    @TableField("project_name")
    private String projectName;

    /** Git远程仓库地址 */
    @TableField("remote_url")
    private String remoteUrl;

    /** 代码托管平台，例如github、gitlab、gitee */
    private String provider;

    /** 仓库命名空间或组织 */
    private String namespace;

    /** 仓库名称 */
    @TableField("repo_name")
    private String repoName;

    /** 默认分支 */
    @TableField("default_branch")
    private String defaultBranch;
}

