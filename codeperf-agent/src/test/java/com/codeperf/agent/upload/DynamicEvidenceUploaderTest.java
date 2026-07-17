package com.codeperf.agent.upload;

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

public class DynamicEvidenceUploaderTest {

    private HttpServer server;
    private String baseUrl;
    private volatile String lastPath;
    private volatile String lastBody;

    @BeforeEach
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/api/tasks/task-1/dynamic-evidence", this::handleUpload);
        server.createContext("/api/dynamic-evidence", this::handleUpload);
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
    public void should_PostDynamicEvidence_When_UploadCalled() throws Exception {
        DynamicEvidenceUploader uploader = new DynamicEvidenceUploader(baseUrl, "task-1");

        uploader.upload("{\"entry\":\"POST /api/orders/report\"}");

        assertEquals("/api/tasks/task-1/dynamic-evidence", lastPath);
        assertTrue(lastBody.contains("\"entry\""));
    }

    @Test
    public void should_PostDynamicEvidenceByCommitIdentity_When_TaskIdMissing() throws Exception {
        DynamicEvidenceUploader uploader = new DynamicEvidenceUploader(baseUrl, "", "demo-app", "dev",
                "git@gitlab.example.com:demo/demo-app.git", "abc123", "master");

        uploader.upload("{\"entryMethod\":\"POST\",\"entryPath\":\"/api/orders/report\"}");

        assertEquals("/api/dynamic-evidence", lastPath);
        assertTrue(lastBody.contains("\"appName\":\"demo-app\""));
        assertTrue(lastBody.contains("\"env\":\"dev\""));
        assertTrue(lastBody.contains("\"remoteUrl\":\"git@gitlab.example.com:demo/demo-app.git\""));
        assertTrue(lastBody.contains("\"commit\":\"abc123\""));
        assertTrue(lastBody.contains("\"branch\":\"master\""));
        assertTrue(lastBody.contains("\"evidence\""));
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        lastPath = exchange.getRequestURI().getPath();
        lastBody = readBody(exchange.getRequestBody());
        byte[] body = "{\"status\":\"DYNAMIC_RECEIVED\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
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
