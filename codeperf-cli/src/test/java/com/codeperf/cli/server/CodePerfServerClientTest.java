package com.codeperf.cli.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodePerfServerClientTest {

    private HttpServer server;
    private String baseUrl;
    private volatile String lastRequestBody;

    @BeforeEach
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/api/tasks", this::handleTaskRequests);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void should_CreateTask_When_ServerReturnsTaskId() throws Exception {
        CodePerfServerClient client = new CodePerfServerClient(baseUrl);

        String taskId = client.createTask("order-service", "abc123", "main", "preprod");

        assertEquals("task-1", taskId);
        assertTrue(lastRequestBody.contains("\"project\":\"order-service\""));
        assertTrue(lastRequestBody.contains("\"commit\":\"abc123\""));
    }

    @Test
    public void should_UploadStaticResult_When_TaskIdProvided() throws Exception {
        CodePerfServerClient client = new CodePerfServerClient(baseUrl);

        client.uploadStaticResult("task-1", "{\"findings\":[]}");

        assertEquals("{\"findings\":[]}", lastRequestBody);
    }

    @Test
    public void should_ReadGate_When_TaskIdProvided() throws Exception {
        CodePerfServerClient client = new CodePerfServerClient(baseUrl);

        GateResult result = client.getGate("task-1");

        assertEquals("task-1", result.getAnalysisTaskId());
        assertEquals("STATIC_RECEIVED", result.getStatus());
        assertEquals("WARN", result.getRiskLevel());
    }

    private void handleTaskRequests(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        lastRequestBody = readBody(exchange.getRequestBody());
        if ("POST".equals(exchange.getRequestMethod()) && "/api/tasks".equals(path)) {
            respond(exchange, "{\"analysisTaskId\":\"task-1\"}");
            return;
        }
        if ("POST".equals(exchange.getRequestMethod()) && "/api/tasks/task-1/static-results".equals(path)) {
            respond(exchange, "{\"status\":\"STATIC_RECEIVED\",\"riskLevel\":\"WARN\"}");
            return;
        }
        if ("GET".equals(exchange.getRequestMethod()) && "/api/tasks/task-1/gate".equals(path)) {
            respond(exchange, "{\"analysisTaskId\":\"task-1\",\"status\":\"STATIC_RECEIVED\",\"riskLevel\":\"WARN\"}");
            return;
        }
        exchange.sendResponseHeaders(404, -1);
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String readBody(InputStream input) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        StringBuilder builder = new StringBuilder();
        while ((read = input.read(buffer)) >= 0) {
            builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
