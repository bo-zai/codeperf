package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * Statement 直接传 SQL 的 execute*(String ...) 拦截。见 docs/02-agent-core.md 第 5.2 节。
 */
public class JdbcStatementAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Argument(0) String sql, @Advice.Enter long startNano) {
        Recorder.recordStatementExecution(sql, startNano);
    }
}

