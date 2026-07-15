package com.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("static_finding")
public class StaticFinding {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分析任务ID，逻辑关联analysis_task.task_id */
    @TableField("task_id")
    private String taskId;

    /** 规则ID，逻辑关联rule_definition.rule_id */
    @TableField("rule_id")
    private String ruleId;

    /** 严重级别 */
    private String severity;
    /** 置信度 */
    private String confidence;

    /** 源码文件路径 */
    @TableField("source_file")
    private String sourceFile;

    /** 风险代码行号 */
    @TableField("line_number")
    private Integer lineNumber;

    /** 循环起始行号 */
    @TableField("loop_start_line")
    private Integer loopStartLine;

    /** 循环结束行号 */
    @TableField("loop_end_line")
    private Integer loopEndLine;

    /** 循环所在方法名 */
    @TableField("loop_method_name")
    private String loopMethodName;

    /** I/O类型，例如DB、HTTP、RPC、SDK */
    @TableField("io_type")
    private String ioType;

    /** 风险范围，例如NEW、HISTORICAL */
    @TableField("risk_scope")
    private String riskScope;

    /** 是否命中本次变更行 */
    @TableField("changed_line")
    private Boolean changedLine;

    /** 引入人姓名 */
    @TableField("introduced_by_name")
    private String introducedByName;

    /** 引入人邮箱 */
    @TableField("introduced_by_email")
    private String introducedByEmail;

    /** 引入风险的提交SHA */
    @TableField("introduced_commit")
    private String introducedCommit;

    /** 引入风险的提交时间 */
    @TableField("introduced_commit_time")
    private String introducedCommitTime;

    /** 证据哈希，用于问题去重 */
    @TableField("evidence_hash")
    private String evidenceHash;

    /** 静态发现原始JSON */
    @TableField("raw_payload")
    private String rawPayload;
}
