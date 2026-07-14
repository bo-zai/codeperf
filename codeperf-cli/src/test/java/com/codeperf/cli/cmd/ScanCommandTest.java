package com.codeperf.cli.cmd;

import com.beust.jcommander.JCommander;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

        int exitCode = command.execute();

        assertEquals(1, exitCode);
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf/report/source-report.json")));
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
            assertEquals("POST", requests.get(1).method);
            assertEquals("/api/tasks/task-1/static-results", requests.get(1).path);
            assertTrue(requests.get(1).body.contains("\"filesScanned\""));
            assertTrue(requests.get(1).body.contains("\"findings\""));
        } finally {
            server.stop(0);
        }
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
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
            byte[] bytes = new byte[8192];
            StringBuilder body = new StringBuilder();
            int read;
            while ((read = input.read(bytes)) >= 0) {
                body.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
            }
            return body.toString();
        }
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
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
