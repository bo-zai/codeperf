package com.cmb.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 动态调用级证据实体。
 */
@Data
@TableName("dynamic_call_evidence")
public class DynamicCallEvidence {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分析任务ID，逻辑关联analysis_task.task_id */
    @TableField("task_id")
    private String taskId;

    /** 请求级证据ID，逻辑关联dynamic_request_evidence.id */
    @TableField("request_evidence_id")
    private Long requestEvidenceId;

    /** 动态原始证据ID，逻辑关联dynamic_evidence.id */
    @TableField("raw_evidence_id")
    private Long rawEvidenceId;

    /** HTTP入口标识 */
    @TableField("entry_key")
    private String entryKey;

    /** 类名 */
    @TableField("class_name")
    private String className;

    /** 方法名 */
    @TableField("method_name")
    private String methodName;

    /** 完整方法名 */
    @TableField("full_method_name")
    private String fullMethodName;

    /** 调用路径 */
    @TableField("call_path")
    private String callPath;

    /** 调用次数 */
    @TableField("call_count")
    private Integer callCount;

    /** 总耗时毫秒 */
    @TableField("total_time_ms")
    private Long totalTimeMs;

    /** 调用节点原始JSON */
    @TableField("raw_payload")
    private String rawPayload;
}
