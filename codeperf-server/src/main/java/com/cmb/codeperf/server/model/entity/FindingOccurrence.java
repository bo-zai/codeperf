package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题出现记录数据库对象。
 */
@Data
@TableName("finding_occurrence")
public class FindingOccurrence {

    /** 主键ID */
    private Long id;

    /** 问题ID，逻辑关联finding_issue.id */
    private Long issueId;

    /** 分析任务ID，逻辑关联analysis_task.task_id */
    private String taskId;

    /** 静态发现ID，逻辑关联static_finding.id */
    private Long findingId;

    /** 出现类型，例如NEW、EXISTING、FIXED */
    private String occurrenceType;

    /** 风险范围 */
    private String riskScope;

    /** 严重级别 */
    private String severity;

    /** 置信度 */
    private String confidence;

    /** 出现记录原始JSON */
    private String rawPayload;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
