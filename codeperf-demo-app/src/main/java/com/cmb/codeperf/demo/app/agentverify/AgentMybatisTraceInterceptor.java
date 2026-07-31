package com.cmb.codeperf.demo.app.agentverify;

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
 * Agent 验证用 MyBatis Interceptor。
 * 企业项目常在这里接入分页、审计、数据权限等逻辑，本类用于复现这些拦截器遮挡业务调用树的场景。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        org.apache.ibatis.cache.CacheKey.class, BoundSql.class})
})
public class AgentMybatisTraceInterceptor implements Interceptor {

    private final AtomicInteger interceptCount = new AtomicInteger();

    /**
     * 拦截 MyBatis 查询执行。
     *
     * @param invocation MyBatis 调用上下文
     * @return 原始 SQL 查询结果
     * @throws Throwable 原始执行异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        interceptCount.incrementAndGet();
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
        // demo 不需要外部属性；保留该方法是为了贴近企业项目 Interceptor 标准结构。
    }

    /**
     * 获取 MyBatis Interceptor 命中次数，方便确认查询确实进入 MyBatis 插件链。
     *
     * @return 拦截次数
     */
    public int getInterceptCount() {
        return interceptCount.get();
    }
}
