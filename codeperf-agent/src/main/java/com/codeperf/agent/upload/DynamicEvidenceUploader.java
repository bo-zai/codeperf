package com.codeperf.agent.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 动态证据上报器。
 * Agent 只负责把预发运行证据发送到服务端，不在目标进程内做报告合并或门禁判断。
 */
public class DynamicEvidenceUploader {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 10000;

    private final String baseUrl;
    private final String analysisTaskId;

    public DynamicEvidenceUploader(String baseUrl, String analysisTaskId) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.analysisTaskId = analysisTaskId;
    }

    /**
     * 上传动态证据 JSON。
     *
     * @param payload 动态证据 JSON
     * @throws IOException 服务端不可达或响应异常时抛出
     */
    public void upload(String payload) throws IOException {
        HttpURLConnection connection = open();
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        int status = connection.getResponseCode();
        String body = readResponseBody(connection, status);
        if (status < 200 || status >= 300) {
            throw new IOException("CodePerf Server 响应异常，status=" + status + ", body=" + body);
        }
    }

    private HttpURLConnection open() throws IOException {
        URL url = new URL(baseUrl + "/api/tasks/" + analysisTaskId + "/dynamic-evidence");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private String readResponseBody(HttpURLConnection connection, int status) throws IOException {
        InputStream input = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (input == null) {
            return "";
        }
        byte[] buffer = new byte[1024];
        int read;
        StringBuilder builder = new StringBuilder();
        try (InputStream in = input) {
            while ((read = in.read(buffer)) >= 0) {
                builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
        }
        return builder.toString();
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("server url must not be blank");
        }
        String normalized = value.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
