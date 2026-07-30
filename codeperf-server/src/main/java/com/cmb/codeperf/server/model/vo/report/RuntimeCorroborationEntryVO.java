package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

/**
 * 动态佐证入口聚合项。
 */
@Data
public class RuntimeCorroborationEntryVO {

    /** HTTP入口，例如GET /demo/orders */
    private String entryKey;

    /** 入口命中的请求次数 */
    private int hitRequestCount;

    /** 该入口下单次请求最大重复调用次数 */
    private int maxRepeatCount;

    /** 该入口下平均重复调用次数 */
    private int avgRepeatCount;

    /** 最近命中的调用路径 */
    private String latestCallPath;
}
