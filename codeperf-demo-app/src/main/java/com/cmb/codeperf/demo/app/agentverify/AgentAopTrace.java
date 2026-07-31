package com.cmb.codeperf.demo.app.agentverify;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent 验证用业务切面标记。
 * 使用注解触发 AOP，贴近企业项目中审计、鉴权、链路追踪等横切逻辑的真实接入方式。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentAopTrace {
}
