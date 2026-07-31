package com.cmb.codeperf.demo.app.support.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 基础配置。
 * Mapper 与插件按真实 Spring Boot 项目方式注册。
 */
@Configuration
@MapperScan("com.cmb.codeperf.demo.app.repository")
public class MybatisConfiguration {

    /**
     * 注册 SQL 审计插件。
     *
     * @return SQL 审计拦截器
     */
    @Bean
    public SqlAuditInterceptor sqlAuditInterceptor() {
        return new SqlAuditInterceptor();
    }
}
