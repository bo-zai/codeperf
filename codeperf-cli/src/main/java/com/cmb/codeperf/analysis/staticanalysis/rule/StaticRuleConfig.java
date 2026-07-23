package com.cmb.codeperf.analysis.staticanalysis.rule;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class StaticRuleConfig {

    private final List<String> ioClassPatterns;

    public StaticRuleConfig(List<String> ioClassPatterns) {
        this.ioClassPatterns = ioClassPatterns == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(ioClassPatterns);
    }

    public static StaticRuleConfig empty() {
        return new StaticRuleConfig(Collections.emptyList());
    }

}

