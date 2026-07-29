package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.CodePerfServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Server 内置报告页面测试。
 * 页面用于消息通知跳转，必须能展示静态风险和动态证据的综合视图。
 */
@SpringBootTest(
        classes = CodePerfServerApplication.class,
        properties = {
                "codeperf.storage.mode=memory",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        })
@AutoConfigureMockMvc
public class ReportPageControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void should_RenderReportList_When_TasksExist() throws Exception {
        String taskId = createTaskWithStaticAndDynamicEvidence();

        mvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CodePerf 管理系统")))
                .andExpect(content().string(containsString("order-service")))
                .andExpect(content().string(containsString(taskId)))
                .andExpect(content().string(containsString("生成时间")))
                .andExpect(content().string(not(containsString("更新："))))
                .andExpect(content().string(containsString("查看报告")));
    }

    @Test
    public void should_RenderReportDetail_When_TaskExists() throws Exception {
        String taskId = createTaskWithStaticAndDynamicEvidence();

        mvc.perform(get("/reports/" + taskId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("综合检测报告")))
                .andExpect(content().string(containsString("AppOrderPreviewService.java")))
                .andExpect(content().string(containsString("LOOP_IO_AMPLIFICATION")))
                .andExpect(content().string(containsString("POST /api/orders/report")))
                .andExpect(content().string(containsString("动态运行证据用于补充静态风险判定")))
                .andExpect(content().string(containsString("运行证据明细")));
    }

    @Test
    public void should_ShowRuntimeCorroboration_ForEachStaticRisk_When_DynamicEvidenceExists() throws Exception {
        String taskId = createTaskWithStaticAndDynamicEvidence();

        mvc.perform(get("/reports/" + taskId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("静态风险佐证")))
                .andExpect(content().string(containsString("DemoOrderController.preview -&gt; AppOrderPreviewService.preview -&gt; DemoOrderMapper.selectByUserId")))
                .andExpect(content().string(containsString("重复调用 3 次")))
                .andExpect(content().string(containsString("已佐证")));
    }

    private String createTaskWithStaticAndDynamicEvidence() throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"project\":\"order-service\","
                                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                                + "\"commit\":\"abc123\","
                                + "\"branch\":\"v1\","
                                + "\"env\":\"dev\","
                                + "\"authorName\":\"Alice Dev\","
                                + "\"authorEmail\":\"alice@example.com\","
                                + "\"commitMessage\":\"add order report\""
                                + "}"))
                .andExpect(status().isOk())
                .andReturn();
        String taskId = extractTaskId(created);

        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"filesScanned\":12,"
                                + "\"findings\":[{"
                                + "\"ruleId\":\"LOOP_IO_AMPLIFICATION\","
                                + "\"severity\":\"WARN\","
                                + "\"confidence\":\"HIGH\","
                                + "\"sourceFile\":\"codeperf-demo-app/src/main/java/com/cmb/codeperf/demo/app/service/AppOrderPreviewService.java\","
                                + "\"evidence\":\"数据库访问调用: orderMapper.selectByUserId(userId)\","
                                + "\"lineNumber\":36,"
                                + "\"loopStartLine\":35,"
                                + "\"loopEndLine\":42,"
                                + "\"ioType\":\"DB\","
                                + "\"loopMethodName\":\"preview\","
                                + "\"loopCallLine\":36,"
                                + "\"ioLine\":36,"
                                + "\"attribution\":{\"riskScope\":\"NEW\",\"introducedByEmail\":\"alice@example.com\"}"
                                + "}],"
                                + "\"parseErrors\":[]"
                                + "}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/dynamic-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"remoteUrl\":\"git@gitlab.company.com:mall/order-service.git\","
                                + "\"commit\":\"abc123\","
                                + "\"branch\":\"v1\","
                                + "\"env\":\"dev\","
                                + "\"appName\":\"order-service\","
                                + "\"evidence\":{"
                                + "\"entryMethod\":\"POST\","
                                + "\"entryPath\":\"/api/orders/report\","
                                + "\"requests\":[{"
                                + "\"httpMethod\":\"POST\","
                                + "\"path\":\"/api/orders/report\","
                                + "\"callTree\":{"
                                + "\"method\":\"ROOT\","
                                + "\"count\":0,"
                                + "\"children\":[{"
                                + "\"method\":\"com.cmb.codeperf.demo.app.controller.DemoOrderController.preview\","
                                + "\"count\":1,"
                                + "\"children\":[{"
                                + "\"method\":\"com.cmb.codeperf.demo.app.service.AppOrderPreviewService.preview\","
                                + "\"count\":1,"
                                + "\"children\":[{"
                                + "\"method\":\"com.cmb.codeperf.demo.app.infrastructure.DemoOrderMapper.selectByUserId\","
                                + "\"count\":3,"
                                + "\"children\":[]"
                                + "}]"
                                + "}]"
                                + "}]"
                                + "}"
                                + "}]"
                                + "}"
                                + "}"))
                .andExpect(status().isOk());
        return taskId;
    }

    private String extractTaskId(MvcResult created) throws Exception {
        String body = created.getResponse().getContentAsString();
        return body.replaceAll(".*\"analysisTaskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
