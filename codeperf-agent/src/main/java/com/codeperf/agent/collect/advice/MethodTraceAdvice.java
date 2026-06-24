package com.codeperf.agent.collect.advice;

import com.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * 方法级插桩 Advice：进方法压栈记时，出方法弹栈累加。
 * 内联到目标方法，仅委托 Recorder 静态方法。见 docs/02-agent-core.md 第 5.1 节。
 */
public class MethodTraceAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter(@Advice.Origin("#t.#m") String signature) {
        return Recorder.enterMethod(signature);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter long token) {
        Recorder.exitMethod(token);
    }
}
