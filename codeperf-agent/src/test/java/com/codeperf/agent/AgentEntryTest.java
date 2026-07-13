package com.codeperf.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class AgentEntryTest {

    @Test
    public void should_NotExposeAgentmain_When_OfficialStartupUsesJavaagent() {
        boolean hasAgentmain = false;
        for (Method method : AgentEntry.class.getDeclaredMethods()) {
            if ("agentmain".equals(method.getName())) {
                hasAgentmain = true;
                break;
            }
        }

        assertFalse(hasAgentmain);
    }
}
