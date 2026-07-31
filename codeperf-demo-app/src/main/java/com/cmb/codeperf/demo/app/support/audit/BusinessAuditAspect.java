package com.cmb.codeperf.demo.app.support.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务审计切面。
 * 该切面模拟企业项目里的统一审计链路，同时为测试暴露命中次数，确认 AOP 代理真实生效。
 */
@Aspect
@Component
public class BusinessAuditAspect {

    private final AtomicInteger auditCount = new AtomicInteger();

    /**
     * 包裹带 {@link BusinessAudit} 的业务方法。
     *
     * @param joinPoint AOP 调用点
     * @return 原业务方法返回值
     * @throws Throwable 原业务异常
     */
    @Around("@annotation(com.cmb.codeperf.demo.app.support.audit.BusinessAudit)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        auditCount.incrementAndGet();
        return joinPoint.proceed();
    }

    /**
     * 获取审计切面命中次数。
     *
     * @return 切面命中次数
     */
    public int getAuditCount() {
        return auditCount.get();
    }
}
