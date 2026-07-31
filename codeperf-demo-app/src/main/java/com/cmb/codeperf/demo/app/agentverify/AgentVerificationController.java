package com.cmb.codeperf.demo.app.agentverify;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 手工验证接口。
 * 访问该接口可触发 Controller -> Spring AOP -> Service -> MyBatis Interceptor -> SQL 的完整链路。
 */
@RestController
@RequestMapping("/demo/agent")
public class AgentVerificationController {

    private final AgentVerificationService agentVerificationService;
    private final AgentAopTraceAspect agentAopTraceAspect;
    private final AgentMybatisTraceInterceptor agentMybatisTraceInterceptor;

    public AgentVerificationController(AgentVerificationService agentVerificationService,
                                       AgentAopTraceAspect agentAopTraceAspect,
                                       AgentMybatisTraceInterceptor agentMybatisTraceInterceptor) {
        this.agentVerificationService = agentVerificationService;
        this.agentAopTraceAspect = agentAopTraceAspect;
        this.agentMybatisTraceInterceptor = agentMybatisTraceInterceptor;
    }

    /**
     * 触发 AOP 与 MyBatis Interceptor 验证链路。
     *
     * @param userIds 用户 ID 列表，未传时使用固定演示数据
     * @return 验证结果与拦截计数
     */
    @GetMapping("/aop-mybatis/orders")
    public Map<String, Object> aopMybatisOrders(
            @RequestParam(value = "userIds", required = false) List<Long> userIds) {
        List<Long> effectiveUserIds = userIds == null || userIds.isEmpty()
                ? Arrays.asList(4101L, 4102L, 4103L, 4104L, 4105L)
                : userIds;
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userIds", effectiveUserIds);
        response.put("rows", agentVerificationService.buildAopMybatisRows(effectiveUserIds));
        response.put("aopTraceCount", agentAopTraceAspect.getTraceCount());
        response.put("mybatisInterceptCount", agentMybatisTraceInterceptor.getInterceptCount());
        return response;
    }
}
