package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 综合报告中的静态风险卡片。
 */
@Data
public class ReportFindingCardVO {

    /** 规则ID */
    private String ruleId;

    /** 严重级别 */
    private String severity;

    /** 置信度 */
    private String confidence;

    /** 源码路径 */
    private String sourceFile;

    /** 静态规则命中的证据描述 */
    private String evidence;

    /** 源码文件名 */
    private String fileName;

    /** 风险行号 */
    private int lineNumber;

    /** 循环起始行 */
    private int loopStartLine;

    /** 循环结束行 */
    private int loopEndLine;

    /** 循环内调用行 */
    private int loopCallLine;

    /** 外部I/O命中行 */
    private int ioLine;

    /** 循环所在方法 */
    private String methodName;

    /** I/O类型 */
    private String ioType;

    /** 风险范围 */
    private String riskScope;

    /** 引入人姓名 */
    private String introducedByName;

    /** 引入人邮箱 */
    private String introducedByEmail;

    /** 引入风险的提交 */
    private String introducedCommit;

    /** 展示用位置 */
    private String location;

    /** 动态佐证状态文案 */
    private String runtimeCorroborationStatus;

    /** 动态佐证结论文案 */
    private String runtimeCorroborationText;

    /** 动态命中的入口请求 */
    private String runtimeEntryKey;

    /** 动态命中的调用路径 */
    private String runtimeCallPath;

    /** 动态命中的方法 */
    private String runtimeMatchedMethod;

    /** 动态命中方法执行次数 */
    private int runtimeRepeatCount;

    /** 动态匹配依据 */
    private String runtimeMatchedReason;

    /** 动态佐证命中请求数 */
    private int runtimeHitRequestCount;

    /** 动态佐证命中入口数 */
    private int runtimeHitEntryCount;

    /** 动态佐证平均重复调用次数 */
    private int runtimeAvgRepeatCount;

    /** 动态佐证主要入口 */
    private String runtimeTopEntryKey;

    /** 动态佐证入口分布 */
    private List<RuntimeCorroborationEntryVO> runtimeTopEntries = new ArrayList<>();
}
