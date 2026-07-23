package com.cmb.codeperf.cli.cmd;

import com.beust.jcommander.JCommander;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScanCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_RunAstScan_When_ConfigAndChangedFileProvided() throws Exception {
        initGitRepo();
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  baseRef: HEAD\n"
                        + "  headRef: --\n"
                        + "  failOn: WARN\n");
        write("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        runGit("add", "src/main/java/com/acme/OrderService.java");

        ScanCommand command = new ScanCommand();
        command.setWorkingDirectoryForTest(tempDir);
        command.setOutputForTest(".codeperf/report/source-report.json");

        CapturedRun capturedRun = captureStdout(command::execute);

        assertEquals(1, capturedRun.exitCode);
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf/report/source-report.json")));
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf/report/source-report.html")));
        String htmlReport = new String(Files.readAllBytes(
                tempDir.resolve(".codeperf/report/source-report.html")), StandardCharsets.UTF_8);
        assertTrue(htmlReport.contains("源码片段"));
        assertTrue(htmlReport.contains("orderMapper.selectById(id);"));
        assertTrue(htmlReport.contains("source-line hit"));
        assertTrue(capturedRun.output.contains("[codeperf] 扫描文件=1，风险总数=1，阻断风险=1，结果=失败"));
        assertTrue(capturedRun.output.contains("[codeperf] 门禁=失败，阻断=1"));
        assertTrue(capturedRun.output.contains("[codeperf] 模块 root：风险=1"));
        assertTrue(capturedRun.output.contains("[codeperf] jsonReport="));
        assertTrue(capturedRun.output.contains("[codeperf] htmlReport="));
        assertTrue(capturedRun.output.contains("[codeperf] htmlReportUrl=file:///"));
        assertTrue(capturedRun.output.contains("source-report.html"));
        assertTrue(capturedRun.output.contains("阻断风险 LOOP_IO_AMPLIFICATION WARN HIGH"));
        assertTrue(capturedRun.output.contains("位置=src/main/java/com/acme/OrderService.java:6"));
        assertTrue(capturedRun.output.contains("方法=buildReport"));
        assertTrue(capturedRun.output.contains("I/O=DB"));
        assertTrue(!capturedRun.output.contains("源码片段"));
    }

    @Test
    public void should_PrintModuleSummaryAndHtmlModuleGroups_When_ModuleConfigProvided() throws Exception {
        initGitRepo();
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  failOn: WARN\n"
                        + "modules:\n"
                        + "  - name: demo-admin\n"
                        + "    sourceRoots:\n"
                        + "      - codeperf-demo-admin/src/main/java\n"
                        + "  - name: demo-app\n"
                        + "    sourceRoots:\n"
                        + "      - codeperf-demo-app/src/main/java\n");
        write("codeperf-demo-admin/src/main/java/com/acme/admin/AdminOrderService.java",
                riskyService("AdminOrderService", "adminMapper.selectById(id)"));
        write("codeperf-demo-app/src/main/java/com/acme/app/AppOrderService.java",
                riskyService("AppOrderService", "appMapper.selectById(id)"));

        ScanCommand command = new ScanCommand();
        JCommander.newBuilder().addObject(command).build().parse("--all");
        command.setWorkingDirectoryForTest(tempDir);
        command.setOutputForTest(".codeperf/report/source-report.json");

        CapturedRun capturedRun = captureStdout(command::execute);

        assertEquals(1, capturedRun.exitCode);
        assertTrue(capturedRun.output.contains("[codeperf] 模块 demo-admin：风险=1"));
        assertTrue(capturedRun.output.contains("[codeperf] 模块 demo-app：风险=1"));
        String htmlReport = new String(Files.readAllBytes(
                tempDir.resolve(".codeperf/report/source-report.html")), StandardCharsets.UTF_8);
        assertTrue(htmlReport.contains("demo-admin"));
        assertTrue(htmlReport.contains("demo-app"));
        assertTrue(htmlReport.contains("class=\"module-block\""));
        assertTrue(htmlReport.contains("class=\"issue-list\""));
    }

    @Test
    public void should_UploadStaticReport_When_UploadFlagProvided() throws Exception {
        initGitRepo();
        List<CapturedRequest> requests = new ArrayList<>();
        HttpServer server = startServer(requests);
        try {
            write(".codeperf.yml",
                    "project: demo\n"
                            + "env: local\n"
                            + "staticScan:\n"
                            + "  sourceRoots:\n"
                            + "    - src/main/java\n"
                            + "  failOn: NONE\n"
                            + "report:\n"
                            + "  local:\n"
                            + "    enabled: false\n"
                            + "  upload:\n"
                            + "    serverUrl: http://127.0.0.1:" + server.getAddress().getPort() + "\n");
            write("src/main/java/com/acme/OrderService.java",
                    "package com.acme;\n"
                            + "class OrderService {\n"
                            + "  private OrderMapper orderMapper;\n"
                            + "  void buildReport(java.util.List<Long> ids) {\n"
                            + "    for (Long id : ids) {\n"
                            + "      orderMapper.selectById(id);\n"
                            + "    }\n"
                            + "  }\n"
                            + "}\n");

            ScanCommand command = new ScanCommand();
            JCommander.newBuilder().addObject(command).build().parse("--all", "--upload");
            command.setWorkingDirectoryForTest(tempDir);

            int exitCode = command.execute();

            assertEquals(1, exitCode);
            assertEquals(2, requests.size());
        assertEquals("POST", requests.get(0).method);
        assertEquals("/api/tasks", requests.get(0).path);
        assertTrue(requests.get(0).body.contains("\"project\":\"demo\""));
        assertTrue(requests.get(0).body.contains("\"remoteUrl\""));
        assertTrue(requests.get(0).body.contains("\"authorEmail\""));
        assertTrue(requests.get(0).body.contains("\"commitMessage\""));
        assertEquals("POST", requests.get(1).method);
            assertEquals("/api/tasks/task-1/static-results", requests.get(1).path);
            assertTrue(requests.get(1).body.contains("\"filesScanned\""));
            assertTrue(requests.get(1).body.contains("\"findings\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void should_ReturnError_When_UploadRequestedWithoutReportServerUrl() throws Exception {
        initGitRepo();
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  failOn: NONE\n"
                        + "report:\n"
                        + "  local:\n"
                        + "    enabled: true\n"
                        + "  upload:\n"
                        + "    enabled: false\n");
        write("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  void buildReport() {\n"
                        + "  }\n"
                        + "}\n");

        ScanCommand command = new ScanCommand();
        JCommander.newBuilder().addObject(command).build().parse("--all", "--upload");
        command.setWorkingDirectoryForTest(tempDir);

        int exitCode = command.execute();

        assertEquals(2, exitCode);
    }

    @Test
    public void should_AttributeFindingToCommitAuthor_When_RiskIntroducedInChangedLines() throws Exception {
        initGitRepo();
        String baseCommit = runGitOutput("rev-parse", "HEAD");
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  baseRef: " + baseCommit + "\n"
                        + "  headRef: HEAD\n"
                        + "  failOn: WARN\n");
        write("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        runGit("add", "src/main/java/com/acme/OrderService.java");
        runGit("-c", "user.name=Alice Dev", "-c", "user.email=alice@example.com",
                "commit", "-m", "add risky report");

        ScanCommand command = new ScanCommand();
        command.setWorkingDirectoryForTest(tempDir);
        command.setOutputForTest(".codeperf/report/source-report.json");

        int exitCode = command.execute();

        assertEquals(1, exitCode);
        String report = new String(Files.readAllBytes(
                tempDir.resolve(".codeperf/report/source-report.json")), StandardCharsets.UTF_8);
        assertTrue(report.contains("\"riskScope\" : \"NEW\""));
        assertTrue(report.contains("\"changedLine\" : true"));
        assertTrue(report.contains("\"attributionConfidence\" : \"HIGH\""));
        assertTrue(report.contains("\"introducedByName\" : \"Alice Dev\""));
        assertTrue(report.contains("\"introducedByEmail\" : \"alice@example.com\""));
        assertTrue(report.contains("\"introducedCommitMessage\" : \"add risky report\""));
    }

    @Test
    public void should_NotFailGate_When_ChangedFileOnlyContainsHistoricalRisk() throws Exception {
        initGitRepo();
        write("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    int retry = 0;\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        runGit("add", "src/main/java/com/acme/OrderService.java");
        runGit("-c", "user.name=Bob Legacy", "-c", "user.email=bob@example.com",
                "commit", "-m", "add existing risky report");
        String baseCommit = runGitOutput("rev-parse", "HEAD");
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  baseRef: " + baseCommit + "\n"
                        + "  headRef: HEAD\n"
                        + "  failOn: WARN\n");
        write("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    int retry = 1;\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        runGit("add", ".codeperf.yml", "src/main/java/com/acme/OrderService.java");
        runGit("commit", "-m", "tune retry value");

        ScanCommand command = new ScanCommand();
        command.setWorkingDirectoryForTest(tempDir);
        command.setOutputForTest(".codeperf/report/source-report.json");

        CapturedRun capturedRun = captureStdout(command::execute);

        assertEquals(0, capturedRun.exitCode);
        String report = new String(Files.readAllBytes(
                tempDir.resolve(".codeperf/report/source-report.json")), StandardCharsets.UTF_8);
        assertTrue(report.contains("\"riskScope\" : \"HISTORICAL\""));
        assertTrue(report.contains("\"changedLine\" : false"));
        assertTrue(report.contains("\"attributionConfidence\" : \"HIGH\""));
        assertTrue(report.contains("\"introducedByName\" : \"Bob Legacy\""));
        assertTrue(report.contains("\"introducedByEmail\" : \"bob@example.com\""));
        assertTrue(report.contains("\"introducedCommitMessage\" : \"add existing risky report\""));
        assertTrue(capturedRun.output.contains("[codeperf] 历史风险数量=1，请查看 HTML 报告详情。"));
        assertTrue(!capturedRun.output.contains("阻断风险 LOOP_IO_AMPLIFICATION"));
    }

    private String riskyService(String className, String ioCall) {
        return "package com.acme;\n"
                + "class " + className + " {\n"
                + "  void buildReport(java.util.List<Long> ids) {\n"
                + "    for (Long id : ids) {\n"
                + "      " + ioCall + ";\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
    }

    private void initGitRepo() throws Exception {
        runGit("init");
        runGit("config", "user.email", "codeperf@example.com");
        runGit("config", "user.name", "CodePerf Test");
        write("README.md", "# demo\n");
        runGit("add", ".");
        runGit("commit", "-m", "init");
    }

    private void write(String file, String content) throws Exception {
        Path path = tempDir.resolve(file);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void runGit(String... args) throws Exception {
        runGitOutput(args);
    }

    private String runGitOutput(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (InputStream input = process.getInputStream()) {
            output = readStream(input);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("git command failed: " + output);
        }
        return output.trim();
    }

    private HttpServer startServer(List<CapturedRequest> requests) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            CapturedRequest request = new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    readBody(exchange));
            requests.add(request);
            if ("/api/tasks".equals(request.path)) {
                writeResponse(exchange, 200, "{\"analysisTaskId\":\"task-1\"}");
                return;
            }
            writeResponse(exchange, 200, "{\"analysisTaskId\":\"task-1\"}");
        });
        server.start();
        return server;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return readStream(input);
        }
    }

    private String readStream(InputStream input) throws IOException {
        byte[] bytes = new byte[8192];
        StringBuilder body = new StringBuilder();
        int read;
        while ((read = input.read(bytes)) >= 0) {
            body.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private CapturedRun captureStdout(CommandRunner runner) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8.name()));
            int exitCode = runner.run();
            return new CapturedRun(exitCode, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("执行测试命令失败", e);
        } finally {
            System.setOut(originalOut);
        }
    }

    private interface CommandRunner {
        int run();
    }

    private static class CapturedRun {
        private final int exitCode;
        private final String output;

        private CapturedRun(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private static class CapturedRequest {
        private final String method;
        private final String path;
        private final String body;

        private CapturedRequest(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body;
        }
    }
}

