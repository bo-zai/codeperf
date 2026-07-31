package com.cmb.codeperf.demo.app.agentverify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 验证 MyBatis 配置。
 * Interceptor 通过 Bean 方式注册，复现企业 Spring Boot 项目最常见的 MyBatis 插件接入方式。
 */
@Configuration
@MapperScan("com.cmb.codeperf.demo.app.agentverify")
public class AgentMybatisConfig {

    /**
     * 注册 MyBatis 查询拦截器。
     *
     * @return 验证用 MyBatis Interceptor
     */
    @Bean
    public AgentMybatisTraceInterceptor agentMybatisTraceInterceptor() {
        return new AgentMybatisTraceInterceptor();
    }
}
