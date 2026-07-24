package com.cmb.codeperf.agent.upload;

import com.cmb.codeperf.agent.logging.AgentLogger;

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
    private final String appName;
    private final String env;
    private final String remoteUrl;
    private final String commit;
    private final String branch;

    public DynamicEvidenceUploader(String baseUrl, String analysisTaskId) {
        this(baseUrl, analysisTaskId, null, null, null, null, null);
    }

    public DynamicEvidenceUploader(String baseUrl, String analysisTaskId, String appName, String env,
                                   String remoteUrl, String commit, String branch) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.analysisTaskId = analysisTaskId;
        this.appName = appName;
        this.env = env;
        this.remoteUrl = remoteUrl;
        this.commit = commit;
        this.branch = branch;
    }

    /**
     * 上传动态证据 JSON。
     *
     * @param payload 动态证据 JSON
     * @throws IOException 服务端不可达或响应异常时抛出
     */
    public void upload(String payload) throws IOException {
        HttpURLConnection connection = open();
        byte[] bytes = requestPayload(payload).getBytes(StandardCharsets.UTF_8);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        AgentLogger.info("dynamic evidence uploading, url=" + uploadUrl() + ", bytes=" + bytes.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        int status = connection.getResponseCode();
        String body = readResponseBody(connection, status);
        if (status < 200 || status >= 300) {
            throw new IOException("CodePerf Server 响应异常，status=" + status + ", body=" + body);
        }
        AgentLogger.info("dynamic evidence uploaded, status=" + status);
    }

    private HttpURLConnection open() throws IOException {
        URL url = new URL(uploadUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private String uploadUrl() {
        if (!isBlank(analysisTaskId)) {
            return baseUrl + "/api/tasks/" + analysisTaskId + "/dynamic-evidence";
        }
        // 企业流水线中 agent.yml 不应每次渲染 taskId；无 taskId 时交给 Server 按 Git 身份关联任务。
        return baseUrl + "/api/dynamic-evidence";
    }

    private String requestPayload(String payload) {
        if (!isBlank(analysisTaskId)) {
            return payload;
        }
        // 保留原始采集 JSON 作为 evidence 子节点，避免动态采集模型变化时频繁调整外层关联字段。
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        appendJsonField(builder, "appName", appName).append(',');
        appendJsonField(builder, "env", env).append(',');
        appendJsonField(builder, "remoteUrl", remoteUrl).append(',');
        appendJsonField(builder, "commit", commit).append(',');
        appendJsonField(builder, "branch", branch).append(',');
        builder.append("\"evidence\":").append(payload == null || payload.trim().isEmpty() ? "{}" : payload);
        builder.append('}');
        return builder.toString();
    }

    private StringBuilder appendJsonField(StringBuilder builder, String name, String value) {
        builder.append('"').append(name).append("\":\"").append(escapeJson(value)).append('"');
        return builder;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                builder.append('\\').append(c);
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else if (c == '\t') {
                builder.append("\\t");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

