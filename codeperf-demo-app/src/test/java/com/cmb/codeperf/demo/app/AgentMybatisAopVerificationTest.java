package com.cmb.codeperf.demo.app;

import com.cmb.codeperf.demo.app.agentverify.AgentMybatisTraceInterceptor;
import com.cmb.codeperf.demo.app.agentverify.AgentVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 验证场景测试。
 * 该测试保证 demo-app 使用真实 Spring AOP 与 MyBatis Interceptor，而不是手写 mock 调用链。
 */
@SpringBootTest
public class AgentMybatisAopVerificationTest {

    @Autowired
    private AgentVerificationService agentVerificationService;

    @Autowired
    private AgentMybatisTraceInterceptor agentMybatisTraceInterceptor;

    @Test
    public void should_InvokeRealMybatisThroughAnnotatedSpringAop_When_VerificationServiceRuns() {
        int before = agentMybatisTraceInterceptor.getInterceptCount();

        int rowCount = agentVerificationService.buildAopMybatisRows(Arrays.asList(4101L, 4102L, 4103L)).size();

        assertEquals(3, rowCount);
        assertTrue(agentMybatisTraceInterceptor.getInterceptCount() >= before + 3);
    }
}
