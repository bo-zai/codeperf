package com.cmb.codeperf.demo.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 本地 demo 启动入口测试。
 * 覆盖核心业务场景，保证本地入口可稳定执行业务聚合流程。
 */
public class LocalDemoApplicationTest {

    @Test
    public void should_RunDemoScenario_When_StartedLocally() {
        LocalDemoApplication.ScenarioResult result = LocalDemoApplication.runScenario();

        assertEquals(3, result.getPreviewCount());
        assertEquals(4, result.getLookupCount());
    }
}

