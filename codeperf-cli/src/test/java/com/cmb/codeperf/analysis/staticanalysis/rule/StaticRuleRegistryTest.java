package com.cmb.codeperf.analysis.staticanalysis.rule;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StaticRuleRegistryTest {

    @Test
    public void should_ContainLoopIoRule_When_DefaultRegistryCreated() {
        StaticRuleRegistry registry = StaticRuleRegistry.defaultRegistry();

        assertTrue(registry.rules().stream()
                .anyMatch(rule -> "loop-io-amplification".equals(rule.id())));
        assertEquals(0, registry.run(new StaticRuleContext(
                Collections.emptyList(),
                "com.example",
                Collections.emptyList(),
                StaticRuleConfig.empty())).size());
    }
}

