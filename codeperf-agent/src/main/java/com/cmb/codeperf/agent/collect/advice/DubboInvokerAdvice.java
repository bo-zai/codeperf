package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * Dubbo Invoker 拦截。
 * 同时兼容 Apache Dubbo 与老版本 Alibaba Dubbo 的 Invoker/Invocation 结构。
 */
public class DubboInvokerAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.This Object invoker,
                            @Advice.Argument(0) Object invocation,
                            @Advice.Enter long startNano) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000L;
        Recorder.recordRpc("DUBBO", serviceName(invoker), invokeString(invocation, "getMethodName"), elapsedMs);
    }

    private static String serviceName(Object invoker) {
        Object type = invoke(invoker, "getInterface");
        if (type instanceof Class) {
            return ((Class<?>) type).getName();
        }
        return type == null ? "" : type.toString();
    }

    private static String invokeString(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value == null ? "" : value.toString();
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable ignored) {
            // Dubbo 版本差异较多，反射失败时只丢弃该字段，避免影响真实 RPC 调用。
            return null;
        }
    }
}
