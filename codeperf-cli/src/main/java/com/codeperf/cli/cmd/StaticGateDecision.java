package com.codeperf.cli.cmd;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 门禁决策结果：封装阻断风险统计和判定结论。
 * <p>
 * 统计维度：
 * <ul>
 *   <li>blocking：阻断风险总数（NEW + MODIFIED）</li>
 *   <li>newlyIntroduced：新增代码引入的风险数</li>
 *   <li>modified：修改代码引入的风险数</li>
 *   <li>historical：历史遗留风险数（不阻断）</li>
 *   <li>unknown：无法归因的风险数</li>
 * </ul>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class StaticGateDecision {
    private final boolean failed;
    private final int blocking;
    private final int newlyIntroduced;
    private final int modified;
    private final int historical;
    private final int unknown;

    public String summary() {
        return "[codeperf] 门禁=" + (failed ? "失败" : "通过")
                + "，阻断=" + blocking
                + "，新增=" + newlyIntroduced
                + "，修改=" + modified
                + "，历史=" + historical
                + "，未归因=" + unknown;
    }
}
