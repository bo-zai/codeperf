package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题生命周期数据库对象。
 */
@Data
@TableName("finding_issue")
public class FindingIssue {

    /** 主键ID */
    private Long id;

    /** 问题唯一标识 */
    private String issueKey;

    /** 代码仓库ID，逻辑关联code_repository.id */
    private Long repositoryId;

    /** 分支名称 */
    private String branchName;

    /** 规则ID，逻辑关联rule_definition.rule_id */
    private String ruleId;

    /** 证据哈希，用于问题归并 */
    private String evidenceHash;

    /** 源码文件路径 */
    private String sourceFile;

    /** 问题代码行号 */
    private Integer lineNumber;

    /** 循环所在方法名 */
    private String loopMethodName;

    /** I/O类型，例如DB、HTTP、RPC、SDK */
    private String ioType;

    /** 问题负责人姓名快照 */
    private String ownerName;

    /** 问题负责人邮箱 */
    private String ownerEmail;

    /** 问题状态：OPEN-未关闭，FIXED-已修复 */
    private String status;

    /** 严重级别 */
    private String severity;

    /** 首次发现任务ID */
    private String firstSeenTaskId;

    /** 最近发现任务ID */
    private String lastSeenTaskId;

    /** 修复确认任务ID */
    private String fixedTaskId;

    /** 静态风险原始JSON */
    private String rawPayload;

    /** 首次发现时间 */
    private LocalDateTime firstSeenAt;

    /** 最近发现时间 */
    private LocalDateTime lastSeenAt;

    /** 修复时间 */
    private LocalDateTime fixedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
