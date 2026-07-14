package com.codeperf.server.infrastructure.persistence.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("static_finding")
public class StaticFindingEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    @TableField("rule_id")
    private String ruleId;

    private String severity;
    private String confidence;

    @TableField("source_file")
    private String sourceFile;

    @TableField("line_number")
    private Integer lineNumber;

    @TableField("loop_start_line")
    private Integer loopStartLine;

    @TableField("loop_end_line")
    private Integer loopEndLine;

    @TableField("loop_method_name")
    private String loopMethodName;

    @TableField("io_type")
    private String ioType;

    @TableField("risk_scope")
    private String riskScope;

    @TableField("changed_line")
    private Boolean changedLine;

    @TableField("introduced_by_name")
    private String introducedByName;

    @TableField("introduced_by_email")
    private String introducedByEmail;

    @TableField("introduced_commit")
    private String introducedCommit;

    @TableField("introduced_commit_time")
    private String introducedCommitTime;

    @TableField("evidence_hash")
    private String evidenceHash;

    @TableField("raw_payload")
    private String rawPayload;
}
