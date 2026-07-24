package com.cmb.codeperf.server.config;

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

    public static final String DEFAULT_LOCAL_ARTIFACT_PATH = "codeperf-agent/target/codeperf-agent.jar";

    private boolean enabled = true;
    private String serverUrl = "http://127.0.0.1:9095";
    private String agentUrl = "http://127.0.0.1:9095/api/agent/artifact";
    private String agentSha256 = "";
    private String localArtifactPath = DEFAULT_LOCAL_ARTIFACT_PATH;
    private String targetPackages = "com.cmb.codeperf.demo";
    private String excludedPackages = "com.cmb.cjtz,com.cmb.checkerframework,com.cmb.bee,com.cmbchina.ugw";
    private String entryMethod = "POST";
    private String entryPath = "/";
    private long slowSqlMs = 500L;
    private long sampleMs = 10L;
    private String mode = "session";

}

