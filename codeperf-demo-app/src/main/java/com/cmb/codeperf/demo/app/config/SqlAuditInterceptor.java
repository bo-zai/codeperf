package com.cmb.codeperf.demo.app.config;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL 审计拦截器。
 * 企业项目常在 MyBatis 插件链中接入分页、审计、数据权限等逻辑；该类用于复现插件代理对调用链的影响。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class})
})
public class SqlAuditInterceptor implements Interceptor {

    private final AtomicInteger queryCount = new AtomicInteger();

    /**
     * 拦截 MyBatis 查询执行。
     *
     * @param invocation MyBatis 调用上下文
     * @return 原始 SQL 查询结果
     * @throws Throwable 原始执行异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        queryCount.incrementAndGet();
        return invocation.proceed();
    }

    /**
     * 创建 MyBatis 插件代理。
     *
     * @param target 被代理对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    /**
     * 接收 MyBatis 插件属性。
     *
     * @param properties 插件属性
     */
    @Override
    public void setProperties(Properties properties) {
        // 本地演示应用不需要外部插件属性；保留该方法是为了符合 MyBatis Interceptor 标准结构。
    }

    /**
     * 获取查询拦截次数。
     *
     * @return 查询拦截次数
     */
    public int getQueryCount() {
        return queryCount.get();
    }
}
