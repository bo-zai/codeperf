package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.CodePerfServerApplication;
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
                "codeperf.agent.install.agent-url=https://oss.example.com/codeperf-agent.jar",
                "codeperf.agent.install.agent-sha256=abc123",
                "codeperf.agent.install.target-packages=com.company.order,com.company.common",
                "codeperf.agent.install.entry-method=POST",
                "codeperf.agent.install.entry-path=/api/orders/report",
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
                        .content("{"
                                + "\"project\":\"order-service\","
                                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                                + "\"commit\":\"abc\","
                                + "\"branch\":\"main\","
                                + "\"env\":\"preprod\","
                                + "\"authorName\":\"Alice Dev\","
                                + "\"authorEmail\":\"alice@example.com\","
                                + "\"commitMessage\":\"add order report\""
                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisTaskId", not(emptyString())))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String taskId = body.replaceAll(".*\"analysisTaskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findings\":[{\"ruleId\":\"LOOP_IO_AMPLIFICATION\",\"severity\":\"WARN\"}]}"))
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
                .andExpect(jsonPath("$.remoteUrl").value("git@gitlab.company.com:mall/order-service.git"))
                .andExpect(jsonPath("$.authorEmail").value("alice@example.com"))
                .andExpect(jsonPath("$.commitMessage").value("add order report"))
                .andExpect(jsonPath("$.staticRiskLevel").value("WARN"))
                .andExpect(jsonPath("$.hasStaticResult").value(true))
                .andExpect(jsonPath("$.hasDynamicEvidence").value(true))
                .andExpect(jsonPath("$.riskLevel").value("WARN"));
    }

    @Test
    public void should_AcceptDynamicEvidenceByIdentity_When_TaskMatchedByCommit() throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"project\":\"order-service\","
                                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                                + "\"commit\":\"abc\","
                                + "\"branch\":\"main\","
                                + "\"env\":\"dev\""
                                + "}"))
                .andExpect(status().isOk())
                .andReturn();

        String taskId = extractTaskId(created);

        mvc.perform(post("/api/dynamic-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                                + "\"commit\":\"abc\","
                                + "\"branch\":\"main\","
                                + "\"env\":\"dev\","
                                + "\"appName\":\"order-service\","
                                + "\"evidence\":{\"entryMethod\":\"POST\",\"entryPath\":\"/api/orders/report\"}"
                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisTaskId").value(taskId))
                .andExpect(jsonPath("$.status").value("DYNAMIC_RECEIVED"));
    }

    @Test
    public void should_ReturnAgentInstallConfig_When_BuildContextPosted() throws Exception {
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
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.serverUrl").value("http://127.0.0.1:9095"))
                .andExpect(jsonPath("$.agentUrl").value("https://oss.example.com/codeperf-agent.jar"))
                .andExpect(jsonPath("$.agentSha256").value("abc123"))
                .andExpect(jsonPath("$.appName").value("order-service"))
                .andExpect(jsonPath("$.env").value("dev"))
                .andExpect(jsonPath("$.targetPackages[0]").value("com.company.order"))
                .andExpect(jsonPath("$.targetPackages[1]").value("com.company.common"))
                .andExpect(jsonPath("$.entry.method").value("POST"))
                .andExpect(jsonPath("$.entry.path").value("/api/orders/report"));
    }

    @Test
    public void should_ReturnStaticReportSummary_When_CliStaticReportUploaded() throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\":\"demo\",\"commit\":\"abc\",\"branch\":\"main\",\"env\":\"local\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String taskId = extractTaskId(created);
        String cliReport = "{"
                + "\"filesScanned\":35,"
                + "\"findings\":[{"
                + "\"ruleId\":\"LOOP_IO_AMPLIFICATION\","
                + "\"severity\":\"WARN\","
                + "\"confidence\":\"HIGH\","
                + "\"sourceFile\":\"src/main/java/com/acme/OrderService.java\","
                + "\"lineNumber\":42,"
                + "\"loopStartLine\":40,"
                + "\"loopEndLine\":45,"
                + "\"ioType\":\"DB\","
                + "\"loopMethodName\":\"buildReport\","
                + "\"loopCallLine\":43,"
                + "\"ioLine\":44,"
                + "\"attribution\":{"
                + "\"riskScope\":\"NEW\","
                + "\"changedLine\":true,"
                + "\"attributionConfidence\":\"HIGH\","
                + "\"introducedByName\":\"Alice Dev\","
                + "\"introducedByEmail\":\"alice@example.com\","
                + "\"introducedCommit\":\"abc123\","
                + "\"introducedCommitTime\":\"1700000000\","
                + "\"introducedCommitMessage\":\"add risky report\""
                + "}"
                + "}],"
                + "\"parseErrors\":[\"Broken.java: parse failed\"]"
                + "}";

        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cliReport))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staticRiskLevel").value("WARN"));

        mvc.perform(get("/api/tasks/" + taskId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project").value("demo"))
                .andExpect(jsonPath("$.commit").value("abc"))
                .andExpect(jsonPath("$.branch").value("main"))
                .andExpect(jsonPath("$.env").value("local"))
                .andExpect(jsonPath("$.staticSummary.filesScanned").value(35))
                .andExpect(jsonPath("$.staticSummary.findingCount").value(1))
                .andExpect(jsonPath("$.staticSummary.parseErrorCount").value(1))
                .andExpect(jsonPath("$.staticSummary.findings[0].sourceFile")
                        .value("src/main/java/com/acme/OrderService.java"))
                .andExpect(jsonPath("$.staticSummary.findings[0].ruleId").value("LOOP_IO_AMPLIFICATION"))
                .andExpect(jsonPath("$.staticSummary.findings[0].lineNumber").value(42))
                .andExpect(jsonPath("$.staticSummary.findings[0].ioType").value("DB"))
                .andExpect(jsonPath("$.staticSummary.findings[0].loopMethodName").value("buildReport"))
                .andExpect(jsonPath("$.staticSummary.findings[0].attribution.riskScope").value("NEW"))
                .andExpect(jsonPath("$.staticSummary.findings[0].attribution.changedLine").value(true))
                .andExpect(jsonPath("$.staticSummary.findings[0].attribution.introducedByEmail")
                        .value("alice@example.com"));
    }

    @Test
    public void should_RejectInvalidStaticReportJson_When_Uploaded() throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\":\"demo\",\"commit\":\"abc\",\"branch\":\"main\",\"env\":\"local\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String taskId = extractTaskId(created);

        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid static result json"));
    }

    @Test
    public void should_RejectStaticReport_When_RuleIdMissing() throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project\":\"demo\",\"commit\":\"abc\",\"branch\":\"main\",\"env\":\"local\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String taskId = extractTaskId(created);

        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findings\":[{\"severity\":\"WARN\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("static finding missing required field: ruleId"));
    }

    private String extractTaskId(MvcResult created) throws Exception {
        String body = created.getResponse().getContentAsString();
        return body.replaceAll(".*\"analysisTaskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}

