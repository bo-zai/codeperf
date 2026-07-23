package com.cmb.codeperf.server.service.impl;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Agent 安装脚本渲染服务。
 * 脚本作为服务端资源随 jar 发布，避免企业流水线依赖仓库源码路径。
 */
@Service
public class AgentInstallScriptService {

    private static final String SCRIPT_RESOURCE = "agent/install.sh";
    private static final String CONFIG_URL_PLACEHOLDER = "__CODEPERF_INSTALL_CONFIG_URL__";

    /**
     * 渲染安装脚本，并把配置接口地址固化为当前 Server 的同源地址。
     *
     * @param request 当前 HTTP 请求
     * @return 可执行 shell 脚本内容
     */
    public String render(HttpServletRequest request) {
        return loadTemplate().replace(CONFIG_URL_PLACEHOLDER, installConfigUrl(request));
    }

    private String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(SCRIPT_RESOURCE);
        try (InputStream input = resource.getInputStream()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取 Agent 安装脚本资源: " + SCRIPT_RESOURCE, e);
        }
    }

    private String installConfigUrl(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (shouldAppendPort(request.getScheme(), port)) {
            builder.append(':').append(port);
        }
        builder.append(request.getContextPath()).append("/api/agent/install-config");
        return builder.toString();
    }

    private boolean shouldAppendPort(String scheme, int port) {
        if ("http".equalsIgnoreCase(scheme) && port == 80) {
            return false;
        }
        if ("https".equalsIgnoreCase(scheme) && port == 443) {
            return false;
        }
        return port > 0;
    }
}

