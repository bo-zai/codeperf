package com.cmb.codeperf.analysis.staticanalysis.rule;

import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class StaticRuleContext {

    private final List<ClassAnalysis> classes;
    private final String targetPackage;
    private final List<String> sourceRoots;
    private final StaticRuleConfig config;

    public StaticRuleContext(List<ClassAnalysis> classes, String targetPackage,
                             List<String> sourceRoots, StaticRuleConfig config) {
        this.classes = classes == null ? Collections.emptyList() : classes;
        this.targetPackage = targetPackage == null ? "" : targetPackage;
        this.sourceRoots = sourceRoots == null ? Collections.emptyList() : sourceRoots;
        this.config = config == null ? StaticRuleConfig.empty() : config;
    }

}

