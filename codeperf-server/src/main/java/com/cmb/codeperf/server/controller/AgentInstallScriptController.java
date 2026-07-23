package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.service.impl.AgentInstallScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Agent 安装脚本下载接口。
 * 企业流水线只需要下载并执行本脚本，配置接口地址由服务端按当前访问地址内置到脚本中。
 */
@Slf4j
@RestController
@RequestMapping("/api/agent/install.sh")
public class AgentInstallScriptController {

    private static final String SCRIPT_FILE_NAME = "codeperf-install.sh";
    private static final MediaType SHELL_SCRIPT_MEDIA_TYPE = new MediaType("text", "x-shellscript");

    private final AgentInstallScriptService service;

    public AgentInstallScriptController(AgentInstallScriptService service) {
        this.service = service;
    }

    /**
     * 下载可直接用于 CI/CD 的安装脚本。
     *
     * @param request 当前 HTTP 请求，用于推导同源 install-config 地址
     * @return 渲染后的 shell 脚本
     */
    @GetMapping
    public ResponseEntity<String> download(HttpServletRequest request) {
        String script = service.render(request);
        log.info("event=codeperf.agent.install_script.download remoteAddr={} scheme={} serverName={} serverPort={} contextPath={}",
                request.getRemoteAddr(), request.getScheme(), request.getServerName(), request.getServerPort(),
                request.getContextPath());
        return ResponseEntity.ok()
                .contentType(SHELL_SCRIPT_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + SCRIPT_FILE_NAME + "\"")
                .body(script);
    }
}

