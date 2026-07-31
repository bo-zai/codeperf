package com.cmb.codeperf.demo.app.agentverify;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 验证切面。
 * 该切面会包裹业务 Service 方法，用于验证 agent 在 Spring AOP 代理存在时仍能采集到下游 MyBatis I/O。
 */
@Aspect
@Component
public class AgentAopTraceAspect {

    private final AtomicInteger traceCount = new AtomicInteger();

    /**
     * 包裹带 {@link AgentAopTrace} 的业务方法。
     *
     * @param joinPoint AOP 调用点
     * @return 原业务方法返回值
     * @throws Throwable 原业务异常
     */
    @Around("@annotation(com.cmb.codeperf.demo.app.agentverify.AgentAopTrace)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        traceCount.incrementAndGet();
        return joinPoint.proceed();
    }

    /**
     * 获取切面命中次数，方便手工接口与自动化测试确认 AOP 已真实生效。
     *
     * @return 切面命中次数
     */
    public int getTraceCount() {
        return traceCount.get();
    }
}
