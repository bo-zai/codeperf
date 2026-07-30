package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * MyBatis Executor 拦截。
 * 使用 MappedStatement.id 还原 Mapper 接口方法，避免企业项目中的 Interceptor/MapperProxy 遮挡真实 Mapper 调用。
 */
public class MybatisExecutorAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Argument(0) Object mappedStatement, @Advice.Enter long startNano) {
        String statementId = invokeString(mappedStatement, "getId");
        String commandType = invokeString(mappedStatement, "getSqlCommandType");
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000L;
        Recorder.recordMybatis(statementId, commandType, elapsedMs);
    }

    private static String invokeString(Object target, String methodName) {
        if (target == null) {
            return "";
        }
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value == null ? "" : value.toString();
        } catch (Throwable ignored) {
            // Advice 不能因框架版本差异影响业务执行，解析失败时放弃本次语义事件字段。
            return "";
        }
    }
}
