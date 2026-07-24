package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.CodePerfServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安装配置默认值回归测试。
 * 这里不注入任何 agent 安装相关环境变量，确保本地启动 Server 后接口默认不会返回空的 agentUrl/targetPackages。
 */
@SpringBootTest(
        classes = CodePerfServerApplication.class,
        properties = {
                "codeperf.storage.mode=memory",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        })
@AutoConfigureMockMvc
public class AgentInstallConfigDefaultTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void should_ReturnNonEmptyDefaults_When_NoAgentEnvProvided() throws Exception {
        mvc.perform(post("/api/agent/install-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"project\":\"order-service\","
                                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                                + "\"commit\":\"abc\","
                                + "\"branch\":\"main\","
                                + "\"env\":\"dev\""
                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentUrl", not(emptyString())))
                .andExpect(jsonPath("$.targetPackages[0]", not(emptyString())))
                .andExpect(jsonPath("$.excludedPackages[0]").value("com.cmb.cjtz"))
                .andExpect(jsonPath("$.excludedPackages[1]").value("com.cmb.checkerframework"))
                .andExpect(jsonPath("$.excludedPackages[2]").value("com.cmb.bee"))
                .andExpect(jsonPath("$.excludedPackages[3]").value("com.cmbchina.ugw"));
    }
}

