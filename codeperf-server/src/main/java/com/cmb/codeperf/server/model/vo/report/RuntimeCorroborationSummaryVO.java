package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单条静态风险对应的动态佐证聚合摘要。
 */
@Data
public class RuntimeCorroborationSummaryVO {

    /** 佐证状态：NOT_HIT、HIT、FREQUENT_HIT、MULTI_ENTRY_HIT、HIGH_AMPLIFICATION */
    private String status;

    /** 是否已有动态运行命中 */
    private boolean matched;

    /** 命中的请求次数 */
    private int hitRequestCount;

    /** 命中的入口数量 */
    private int hitEntryCount;

    /** 单次请求内最大重复调用次数 */
    private int maxRepeatCount;

    /** 单次请求内平均重复调用次数 */
    private int avgRepeatCount;

    /** 主要入口，按最大重复调用和命中次数综合选择 */
    private String topEntryKey;

    /** 最近命中的入口 */
    private String latestEntryKey;

    /** 最近命中的调用路径 */
    private String latestCallPath;

    /** 最近命中的方法 */
    private String latestMatchedMethod;

    /** 匹配依据 */
    private String matchedReason;

    /** 面向页面展示的结论文案 */
    private String text;

    /** 入口维度聚合结果 */
    private List<RuntimeCorroborationEntryVO> topEntries = new ArrayList<>();
}
