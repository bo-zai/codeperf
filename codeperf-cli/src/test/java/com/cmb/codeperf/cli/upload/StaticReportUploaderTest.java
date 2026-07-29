package com.cmb.codeperf.cli.upload;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StaticReportUploaderTest {

    @Test
    public void should_ApplyConfiguredTimeouts_When_PostingStaticReport() throws Exception {
        List<FakeConnection> connections = new ArrayList<>();
        StaticReportUploader uploader = new StaticReportUploader(url -> {
            FakeConnection connection = new FakeConnection(new URL(url));
            connections.add(connection);
            return connection;
        });
        StaticReportUploadRequest request = new StaticReportUploadRequest(
                "demo",
                "local",
                "abc",
                "main",
                "git@example.com:demo.git",
                "Alice",
                "alice@example.com",
                "2026-07-29 19:30:00",
                "Alice",
                "alice@example.com",
                "scan",
                "{\"findings\":[]}");

        String taskId = uploader.upload("http://127.0.0.1:9095", request, 7000, 120000);

        assertEquals("task-1", taskId);
        assertEquals(2, connections.size());
        assertEquals(7000, connections.get(0).getConnectTimeout());
        assertEquals(120000, connections.get(0).getReadTimeout());
        assertEquals(7000, connections.get(1).getConnectTimeout());
        assertEquals(120000, connections.get(1).getReadTimeout());
    }

    private static class FakeConnection extends HttpURLConnection {

        private String path;

        private FakeConnection(URL url) {
            super(url);
            this.path = url.getPath();
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public OutputStream getOutputStream() {
            return new java.io.ByteArrayOutputStream();
        }

        @Override
        public int getResponseCode() {
            return HTTP_OK;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            String body = "/api/tasks".equals(path)
                    ? "{\"analysisTaskId\":\"task-1\"}"
                    : "{\"analysisTaskId\":\"task-1\"}";
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }
    }
}
