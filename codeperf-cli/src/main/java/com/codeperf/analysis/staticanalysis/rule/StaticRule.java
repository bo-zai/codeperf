package com.codeperf.analysis.staticanalysis.rule;

import com.codeperf.analysis.staticanalysis.StaticFinding;

import java.util.List;

public interface StaticRule {
    String id();

    String displayName();

    List<StaticFinding> analyze(StaticRuleContext context);
}
