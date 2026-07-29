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
 * 分支综合报告测试。
 * 真实研发流程中多人分别 push，流水线只代表当前分支运行结果，页面必须按风险引入人聚合。
 */
@SpringBootTest(
        classes = CodePerfServerApplication.class,
        properties = {
                "codeperf.storage.mode=memory",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        })
@AutoConfigureMockMvc
public class BranchReportPageControllerTest {

    private static final String REMOTE_URL = "git@gitlab.company.com:mall/order-service.git";

    @Autowired
    private MockMvc mvc;

    @Test
    public void should_RenderBranchReportGroupedByIntroducer_When_MultipleDevelopersPushSameBranch() throws Exception {
        createStaticTask("commit-a", "develop", "Alice Dev", "alice@example.com",
                "AppOrderPreviewService.java", "preview", "orderMapper.selectByUserId(userId)");
        createStaticTask("commit-b", "develop", "Bob Dev", "bob@example.com",
                "DemoCheckoutService.java", "loadCheckoutSnapshot", "userRepository.findUserById(userId)");
        String buildTaskId = createTask("merge-commit", "develop", "Build User", "builder@example.com");

        mvc.perform(post("/api/dynamic-evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"remoteUrl\":\"" + REMOTE_URL + "\","
                                + "\"commit\":\"merge-commit\","
                                + "\"branch\":\"develop\","
                                + "\"env\":\"dev\","
                                + "\"appName\":\"order-service\","
                                + "\"evidence\":{\"requests\":[{\"httpMethod\":\"POST\",\"path\":\"/api/orders/report\","
                                + "\"callTree\":{\"method\":\"ROOT\",\"children\":["
                                + "{\"method\":\"com.cmb.demo.AppOrderPreviewService.preview\",\"count\":1,"
                                + "\"children\":[{\"method\":\"com.cmb.demo.OrderMapper.selectByUserId\",\"count\":3,\"children\":[]}]},"
                                + "{\"method\":\"com.cmb.demo.DemoCheckoutService.loadCheckoutSnapshot\",\"count\":1,"
                                + "\"children\":[{\"method\":\"com.cmb.demo.UserRepository.findUserById\",\"count\":2,\"children\":[]}]}"
                                + "]}}]}}"))
                .andExpect(status().isOk());

        mvc.perform(get("/reports/branches/latest")
                        .param("remoteUrl", REMOTE_URL)
                        .param("branch", "develop")
                        .param("env", "dev"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("分支综合报告")))
                .andExpect(content().string(containsString(buildTaskId)))
                .andExpect(content().string(containsString("Alice Dev")))
                .andExpect(content().string(containsString("Bob Dev")))
                .andExpect(content().string(containsString("AppOrderPreviewService.java")))
                .andExpect(content().string(containsString("DemoCheckoutService.java")))
                .andExpect(content().string(containsString("已运行命中")))
                .andExpect(content().string(containsString("重复调用 3 次")));
    }

    @Test
    public void should_CloseIssueOnlyForScannedFile_When_StaticRiskDisappearsInLaterScan() throws Exception {
        createStaticTask("commit-old-a", "develop-fix", "Alice Dev", "alice@example.com",
                "AppOrderPreviewService.java", "preview", "orderMapper.selectByUserId(userId)");
        createStaticTask("commit-old-b", "develop-fix", "Bob Dev", "bob@example.com",
                "DemoCheckoutService.java", "loadCheckoutSnapshot", "userRepository.findUserById(userId)");
        String fixedTaskId = createTask("commit-fixed-a", "develop-fix", "Alice Dev", "alice@example.com");

        mvc.perform(post("/api/tasks/" + fixedTaskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filesScanned\":1,"
                                + "\"scannedSourceFiles\":[\"codeperf-demo-app/src/main/java/com/cmb/demo/AppOrderPreviewService.java\"],"
                                + "\"findings\":[],\"parseErrors\":[]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/reports/branches/latest")
                        .param("remoteUrl", REMOTE_URL)
                        .param("branch", "develop-fix")
                        .param("env", "dev"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Alice Dev"))))
                .andExpect(content().string(not(containsString("AppOrderPreviewService.java"))))
                .andExpect(content().string(containsString("Bob Dev")))
                .andExpect(content().string(containsString("DemoCheckoutService.java")));
    }

    private void createStaticTask(String commit, String branch, String authorName, String authorEmail,
                                  String sourceFile, String methodName, String evidence) throws Exception {
        String taskId = createTask(commit, branch, authorName, authorEmail);
        mvc.perform(post("/api/tasks/" + taskId + "/static-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filesScanned\":12,\"findings\":[{"
                                + "\"ruleId\":\"LOOP_IO_AMPLIFICATION\","
                                + "\"severity\":\"WARN\","
                                + "\"confidence\":\"HIGH\","
                                + "\"sourceFile\":\"codeperf-demo-app/src/main/java/com/cmb/demo/" + sourceFile + "\","
                                + "\"evidence\":\"数据库访问调用: " + evidence + "\","
                                + "\"lineNumber\":36,"
                                + "\"loopStartLine\":35,"
                                + "\"loopEndLine\":42,"
                                + "\"ioType\":\"DB\","
                                + "\"loopMethodName\":\"" + methodName + "\","
                                + "\"attribution\":{\"riskScope\":\"NEW\","
                                + "\"introducedByName\":\"" + authorName + "\","
                                + "\"introducedByEmail\":\"" + authorEmail + "\","
                                + "\"introducedCommit\":\"" + commit + "\"}"
                                + "}],\"parseErrors\":[]}"))
                .andExpect(status().isOk());
    }

    private String createTask(String commit, String branch, String authorName, String authorEmail) throws Exception {
        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"project\":\"order-service\","
                                + "\"remoteUrl\":\"" + REMOTE_URL + "\","
                                + "\"commit\":\"" + commit + "\","
                                + "\"branch\":\"" + branch + "\","
                                + "\"env\":\"dev\","
                                + "\"authorName\":\"" + authorName + "\","
                                + "\"authorEmail\":\"" + authorEmail + "\""
                                + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return extractTaskId(created);
    }

    private String extractTaskId(MvcResult created) throws Exception {
        String body = created.getResponse().getContentAsString();
        return body.replaceAll(".*\"analysisTaskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
