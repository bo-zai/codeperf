package com.codeperf.analysis.source.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
