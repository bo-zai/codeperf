package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.source.SourceFinding;

import java.util.List;

public interface SourceRule {
    List<SourceFinding> analyze(SourceRuleContext context);
}
