package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * 入口识别 Advice：织入 DispatcherServlet#doDispatch。第一个参数是 HttpServletRequest，
 * 交给 Recorder 反射读取 method/URI 并匹配 entry。见 docs/02-agent-core.md 第 6 节。
 */
public class EntryAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static boolean enter(@Advice.Argument(0) Object request) {
        return Recorder.tryStartRequest(request);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter boolean started) {
        if (started) {
            Recorder.finishRequest();
        }
    }
}

