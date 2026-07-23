package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * PreparedStatement 无参 execute()/executeQuery()/executeUpdate() 拦截。
 * SQL 文本来自 prepareStatement 时由 {@link JdbcPrepareBindAdvice} 登记的绑定。
 * 见 docs/02-agent-core.md 第 5.2 节。
 */
public class JdbcPreparedExecAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.This Object preparedStatement, @Advice.Enter long startNano) {
        Recorder.recordPreparedExecution(preparedStatement, startNano);
    }
}

