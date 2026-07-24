package com.cmb.codeperf.agent.collect;

import com.cmb.codeperf.agent.logging.AgentLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

/**
 * ByteBuddy 插桩事件监听器。
 * 企业应用里常见 optional/provided 依赖，单个类解析失败不能影响应用启动和整体采集。
 */
public class CodePerfAgentListener implements AgentBuilder.Listener {

    @Override
    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        // 发现类的频率极高，默认不打印，避免污染业务启动日志。
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                                 JavaModule module, boolean loaded, DynamicType dynamicType) {
        AgentLogger.info("instrumentation transformed, class=" + typeDescription.getName());
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
        // 被忽略的类数量很大，不打印是为了保持日志可读。
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        AgentLogger.error("instrumentation skipped, class=" + safe(typeName)
                + ", error=" + throwable.getClass().getSimpleName()
                + ", message=" + safe(throwable.getMessage()));
    }

    @Override
    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        // 每个类都会触发完成事件，默认不打印。
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
