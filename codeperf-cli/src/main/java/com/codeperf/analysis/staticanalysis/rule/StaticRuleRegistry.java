package com.codeperf.analysis.staticanalysis.rule;

import com.codeperf.analysis.staticanalysis.StaticFinding;
import com.codeperf.analysis.staticanalysis.rules.LoopIoAmplificationRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticRuleRegistry {

    private final List<StaticRule> rules;

    public StaticRuleRegistry(List<StaticRule> rules) {
        this.rules = rules == null ? Collections.emptyList() : Collections.unmodifiableList(rules);
    }

    public static StaticRuleRegistry defaultRegistry() {
        List<StaticRule> defaults = new ArrayList<>();
        defaults.add(new LoopIoAmplificationRule());
        return new StaticRuleRegistry(defaults);
    }

    public List<StaticRule> rules() {
        return rules;
    }

    public List<StaticFinding> run(StaticRuleContext context) {
        List<StaticFinding> findings = new ArrayList<>();
        for (StaticRule rule : rules) {
            findings.addAll(rule.analyze(context));
        }
        return findings;
    }
}
