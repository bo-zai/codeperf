package com.cmb.codeperf.analysis.staticanalysis.rule;

import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;

import java.util.List;

public interface StaticRule {
    String id();

    String displayName();

    List<StaticFinding> analyze(StaticRuleContext context);
}

