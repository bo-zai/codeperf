package com.codeperf.server.api;

import com.codeperf.server.CodePerfServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = CodePerfServerApplication.class,
        properties = {
                "codeperf.storage.mode=memory",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        })
@AutoConfigureMockMvc
public class TaskApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void should_AcceptStaticAndDynamicEvidence_When_TaskCreated() throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\":\"order-service\",\"commit\":\"abc\",\"branch\":\"main\",\"env\":\"preprod\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisTaskId", not(emptyString())))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String taskId = body.replaceAll(".*\"analysisTaskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findings\":[{\"severity\":\"WARN\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("WARN"));

        mvc.perform(post("/api/tasks/" + taskId + "/dynamic-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entry\":\"POST /api/orders/report\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DYNAMIC_RECEIVED"));

        mvc.perform(get("/api/tasks/" + taskId + "/gate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisTaskId").value(taskId))
                .andExpect(jsonPath("$.riskLevel").value("WARN"));

        mvc.perform(get("/api/tasks/" + taskId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisTaskId").value(taskId))
                .andExpect(jsonPath("$.staticRiskLevel").value("WARN"))
                .andExpect(jsonPath("$.hasStaticResult").value(true))
                .andExpect(jsonPath("$.hasDynamicEvidence").value(true))
                .andExpect(jsonPath("$.riskLevel").value("WARN"));
    }
}
