package com.codeperf.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 规则定义数据库对象。
 */
@Data
@TableName("rule_definition")
public class RuleDefinition {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则唯一标识 */
    @TableField("rule_id")
    private String ruleId;

    /** 规则名称 */
    @TableField("rule_name")
    private String ruleName;

    /** 规则分类 */
    private String category;

    /** 检测类型，取值为STATIC或DYNAMIC */
    @TableField("detector_type")
    private String detectorType;

    /** 默认严重级别 */
    @TableField("default_severity")
    private String defaultSeverity;

    /** 是否启用 */
    private Boolean enabled;

    /** 规则版本 */
    private String version;

    /** 规则说明 */
    private String description;

    /** 修复建议 */
    private String remediation;
}
