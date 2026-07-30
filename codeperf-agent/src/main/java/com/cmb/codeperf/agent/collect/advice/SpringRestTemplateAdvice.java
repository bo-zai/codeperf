package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * Spring RestTemplate 拦截。
 * 记录 HTTP 方法和目标 URI，作为循环内 HTTP 调用放大的动态语义证据。
 */
public class SpringRestTemplateAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Argument(0) Object uri,
                            @Advice.Origin("#m") String methodName,
                            @Advice.Enter long startNano) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000L;
        Recorder.recordHttp("SPRING_REST_TEMPLATE", stringValue(methodName), stringValue(uri), elapsedMs);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
