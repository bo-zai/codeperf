package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.source.SourceFinding;

import java.util.List;

/**
 * 源码规则接口：定义源码分析的契约。
 * <p>
 * 实现类：
 * <ul>
 *   <li>LoopIoAmplificationAstRule：检测循环内的直接/间接 I/O 调用</li>
 * </ul>
 * <p>
 * 扩展方式：实现此接口，添加到 SourceRuleRegistry.defaultRegistry()。
 */
public interface SourceRule {
    /**
     * 分析源码上下文，返回检测到的发现列表。
     *
     * @param context 源码规则上下文，包含 CompilationUnit、类索引、配置等
     * @return 发现列表，空列表表示未检测到风险
     */
    List<SourceFinding> analyze(SourceRuleContext context);
}
