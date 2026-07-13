package com.codeperf.agent.upload;

import com.codeperf.agent.session.SessionData;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicEvidenceReporterTest {

    private HttpServer server;
    private String baseUrl;
    private volatile String lastBody;

    @BeforeEach
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/api/tasks/task-1/dynamic-evidence", this::handleUpload);
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
    public void should_UploadSessionJson_When_ReportCalled() throws Exception {
        SessionData session = new SessionData();
        session.setEntryMethod("POST");
        session.setEntryPath("/api/orders/report");

        DynamicEvidenceReporter reporter = new DynamicEvidenceReporter(
                new DynamicEvidenceUploader(baseUrl, "task-1"));

        reporter.report(session);

        assertTrue(lastBody.contains("\"entryMethod\""));
        assertTrue(lastBody.contains("\"/api/orders/report\""));
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
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
