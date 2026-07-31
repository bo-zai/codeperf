package com.cmb.codeperf.demo.app;

import com.cmb.codeperf.demo.app.service.OrderFulfillmentService;
import com.cmb.codeperf.demo.app.support.mybatis.SqlAuditInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单履约集成测试。
 * 该测试保证 demo-app 使用真实 Spring AOP 与 MyBatis Interceptor，而不是手写 mock 调用链。
 */
@SpringBootTest
public class OrderFulfillmentIntegrationTest {

    @Autowired
    private OrderFulfillmentService orderFulfillmentService;

    @Autowired
    private SqlAuditInterceptor sqlAuditInterceptor;

    @Test
    public void should_InvokeRealMybatisThroughAnnotatedSpringAop_When_FulfillmentServiceRuns() {
        int before = sqlAuditInterceptor.getQueryCount();

        int rowCount = orderFulfillmentService.buildFulfillmentRows(Arrays.asList(4101L, 4102L, 4103L)).size();

        assertEquals(3, rowCount);
        assertTrue(sqlAuditInterceptor.getQueryCount() >= before + 3);
    }
}
