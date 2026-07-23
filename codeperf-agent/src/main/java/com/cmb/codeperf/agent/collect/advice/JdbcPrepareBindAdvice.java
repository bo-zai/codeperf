package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

/**
 * Connection#prepareStatement(String sql, ...) 拦截：把返回的 PreparedStatement 与其 SQL 绑定，
 * 供后续无参 execute* 还原 SQL 文本。见 docs/02-agent-core.md 第 5.2 节。
 */
public class JdbcPrepareBindAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(@Advice.Argument(0) String sql, @Advice.Return Object preparedStatement) {
        Recorder.bindPrepared(preparedStatement, sql);
    }
}

