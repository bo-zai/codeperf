package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 静态风险与动态证据佐证关系实体。
 */
@Data
@TableName("static_dynamic_corroboration")
public class StaticDynamicCorroboration {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分析任务ID，逻辑关联analysis_task.task_id */
    @TableField("task_id")
    private String taskId;

    /** 静态风险ID，逻辑关联static_finding.id */
    @TableField("static_finding_id")
    private Long staticFindingId;

    /** 问题ID，逻辑关联finding_issue.id */
    @TableField("finding_issue_id")
    private Long findingIssueId;

    /** 佐证状态 */
    @TableField("status")
    private String status;

    /** 命中请求数 */
    @TableField("hit_request_count")
    private Integer hitRequestCount;

    /** 命中入口数 */
    @TableField("hit_entry_count")
    private Integer hitEntryCount;

    /** 最大重复调用次数 */
    @TableField("max_repeat_count")
    private Integer maxRepeatCount;

    /** 平均重复调用次数 */
    @TableField("avg_repeat_count")
    private Integer avgRepeatCount;

    /** 主要入口 */
    @TableField("top_entry_key")
    private String topEntryKey;

    /** 佐证原因 */
    @TableField("reason")
    private String reason;
}
