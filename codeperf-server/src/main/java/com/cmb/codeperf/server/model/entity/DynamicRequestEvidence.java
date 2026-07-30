package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 动态请求级证据实体。
 */
@Data
@TableName("dynamic_request_evidence")
public class DynamicRequestEvidence {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分析任务ID，逻辑关联analysis_task.task_id */
    @TableField("task_id")
    private String taskId;

    /** 动态原始证据ID，逻辑关联dynamic_evidence.id */
    @TableField("raw_evidence_id")
    private Long rawEvidenceId;

    /** 环境名称 */
    @TableField("env_name")
    private String envName;

    /** 应用名称 */
    @TableField("app_name")
    private String appName;

    /** HTTP请求方法 */
    @TableField("entry_method")
    private String entryMethod;

    /** HTTP请求路径 */
    @TableField("entry_path")
    private String entryPath;

    /** HTTP入口标识，例如GET /demo/orders */
    @TableField("entry_key")
    private String entryKey;

    /** 请求总耗时毫秒 */
    @TableField("wall_time_ms")
    private Long wallTimeMs;

    /** 请求级原始JSON */
    @TableField("raw_payload")
    private String rawPayload;
}
