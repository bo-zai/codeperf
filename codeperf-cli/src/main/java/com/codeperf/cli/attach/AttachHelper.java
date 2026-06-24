package com.codeperf.cli.attach;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过反射加载 JDK8 的 {@code $JAVA_HOME/lib/tools.jar}，调用 Attach API 把 agent 挂到目标进程。
 * 见 docs/03-cli.md 第 3 节。要求 CLI 运行在 JDK（非 JRE）上。
 */
public final class AttachHelper {

    private AttachHelper() {
    }

    public static void attach(String pid, String agentJarPath, String agentArgs) throws Exception {
        File toolsJar = locateToolsJar();
        if (toolsJar == null) {
            throw new IllegalStateException(
                    "找不到 tools.jar。请用 JDK（非 JRE）运行 CLI，例如 D:\\Java8\\jdk1.8.0_341\\bin\\java。"
                            + " 已查找 java.home=" + System.getProperty("java.home")
                            + " 与 JAVA_HOME=" + System.getenv("JAVA_HOME"));
        }

        URL[] urls = {toolsJar.toURI().toURL()};
        try (URLClassLoader loader = new URLClassLoader(urls, AttachHelper.class.getClassLoader())) {
            Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");
            Method attach = vmClass.getMethod("attach", String.class);
            Method loadAgent = vmClass.getMethod("loadAgent", String.class, String.class);
            Method detach = vmClass.getMethod("detach");

            Object vm = attach.invoke(null, pid);
            try {
                loadAgent.invoke(vm, agentJarPath, agentArgs);
            } finally {
                detach.invoke(vm);
            }
        }
    }

    private static File locateToolsJar() {
        List<String> bases = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            bases.add(javaHome);
        }
        String envHome = System.getenv("JAVA_HOME");
        if (envHome != null) {
            bases.add(envHome);
        }
        for (String base : bases) {
            File a = new File(base, "lib/tools.jar");          // JDK 根
            if (a.isFile()) {
                return a;
            }
            File b = new File(base, "../lib/tools.jar");        // java.home 指向 jre 时
            if (b.isFile()) {
                return b;
            }
        }
        return null;
    }
}
