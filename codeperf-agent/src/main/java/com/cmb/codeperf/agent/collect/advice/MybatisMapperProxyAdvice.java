package com.cmb.codeperf.agent.collect.advice;

import com.cmb.codeperf.agent.collect.Recorder;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * MyBatis Mapper 代理拦截。
 * 企业项目常通过 MyBatis 插件、MyBatis-Plus 或 JDK 代理执行 Mapper，Executor 调用树可能被拦截器遮挡。
 * 在 MapperProxy.invoke 层记录接口方法，可以稳定还原静态扫描中的 mapper.xxx(...) 证据。
 */
public class MybatisMapperProxyAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void exit(@Advice.Argument(1) Object mapperMethod, @Advice.Enter long startNano) {
        if (!(mapperMethod instanceof Method)) {
            return;
        }
        Method method = (Method) mapperMethod;
        if (Object.class.equals(method.getDeclaringClass())) {
            return;
        }
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000L;
        Recorder.recordMybatis(method.getDeclaringClass().getName() + "." + method.getName(), "MAPPER", elapsedMs);
    }
}
