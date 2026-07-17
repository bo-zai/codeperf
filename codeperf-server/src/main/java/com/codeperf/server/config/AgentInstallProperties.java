package com.codeperf.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CodePerf Agent 安装配置。
 * 这部分配置由服务端统一维护，脚本只负责拉取，不再把安装细节写死在 DevOps 命令里。
 */
@Component
@ConfigurationProperties(prefix = "codeperf.agent.install")
@Data
public class AgentInstallProperties {

    private boolean enabled = true;
    private String serverUrl = "http://127.0.0.1:9095";
    private String agentUrl = "";
    private String agentSha256 = "";
    private String targetPackages = "";
    private String entryMethod = "POST";
    private String entryPath = "/";
    private long slowSqlMs = 500L;
    private long sampleMs = 10L;
    private String mode = "session";

}
