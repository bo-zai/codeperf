package com.codeperf.server.controller;

import com.codeperf.server.CodePerfServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 健康探针测试。
 * 这里验证接口始终可用且不依赖数据库，适合容器探活使用。
 */
@SpringBootTest(classes = CodePerfServerApplication.class)
@AutoConfigureMockMvc
public class HealthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void should_ReturnOk_When_HealthChecked() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
