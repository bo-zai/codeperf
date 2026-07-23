package com.cmb.codeperf.analysis.source.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则注册表：管理所有 SourceRule 并提供默认规则集。
 * <p>
 * 默认规则：
 * <ul>
 *   <li>LoopIoAmplificationAstRule：循环 I/O 放大检测</li>
 * </ul>
 * <p>
 * 扩展方式：构造时传入自定义规则列表。
 */
public class SourceRuleRegistry {

    private final List<SourceRule> rules;

    public SourceRuleRegistry(List<SourceRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public static SourceRuleRegistry defaultRegistry() {
        List<SourceRule> defaults = new ArrayList<>();
        defaults.add(new LoopIoAmplificationAstRule());
        return new SourceRuleRegistry(defaults);
    }

    public List<SourceRule> rules() {
        return Collections.unmodifiableList(rules);
    }
}

