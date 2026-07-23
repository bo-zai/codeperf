package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.service.impl.AgentInstallScriptService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 安装脚本下载接口测试。
 * 验证流水线不需要额外配置 CODEPERF_INSTALL_CONFIG_URL，脚本会默认使用当前 Server 地址。
 */
public class AgentInstallScriptControllerTest {

    @Test
    public void should_RenderInstallScript_When_DownloadedFromServer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("codeperf-server");
        request.setServerPort(9095);
        AgentInstallScriptController controller = new AgentInstallScriptController(new AgentInstallScriptService());

        ResponseEntity<String> response = controller.download(request);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().startsWith("#!/usr/bin/env bash"));
        assertTrue(response.getBody().contains("CODEPERF_INSTALL_CONFIG_URL=\"${CODEPERF_INSTALL_CONFIG_URL:-"
                + "http://codeperf-server:9095/api/agent/install-config}\""));
        assertFalse(response.getBody().contains("__CODEPERF_INSTALL_CONFIG_URL__"));
        assertEquals("attachment; filename=\"codeperf-install.sh\"",
                response.getHeaders().getFirst("Content-Disposition"));
    }
}

