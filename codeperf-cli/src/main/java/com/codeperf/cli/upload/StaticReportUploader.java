package com.codeperf.cli.upload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报告上传器：HTTP POST 扫描报告到 CodePerf 服务端。
 * <p>
 * 上传流程：
 * <ol>
 *   <li>POST /api/tasks 创建任务，获取 taskId</li>
 *   <li>POST /api/tasks/{taskId}/static-results 上传报告 JSON</li>
 * </ol>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>CI 集成：每次构建上传报告，建立历史趋势</li>
 *   <li>团队协作：在 Web 界面审查报告，分配问题负责人</li>
 * </ul>
 */
public class StaticReportUploader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String upload(String serverUrl, StaticReportUploadRequest request) throws IOException {
        // 两阶段上传：先创建任务获取 taskId，再上传报告内容
        String taskId = createTask(serverUrl, request);
        post(serverUrl + "/api/tasks/" + taskId + "/static-results", request.getReportJson());
        return taskId;
    }

    private String createTask(String serverUrl, StaticReportUploadRequest request) throws IOException {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("project", request.getProject());
        payload.put("remoteUrl", request.getRemoteUrl());
        payload.put("commit", request.getCommit());
        payload.put("branch", request.getBranch());
        payload.put("env", request.getEnv());
        payload.put("authorName", request.getAuthorName());
        payload.put("authorEmail", request.getAuthorEmail());
        payload.put("authorTime", request.getAuthorTime());
        payload.put("committerName", request.getCommitterName());
        payload.put("committerEmail", request.getCommitterEmail());
        payload.put("commitMessage", request.getCommitMessage());
        String response = post(serverUrl + "/api/tasks", objectMapper.writeValueAsString(payload));
        JsonNode node = objectMapper.readTree(response);
        JsonNode taskId = node.get("analysisTaskId");
        if (taskId == null || taskId.asText().trim().isEmpty()) {
            throw new IOException("server response missing analysisTaskId");
        }
        return taskId.asText();
    }

    private String post(String url, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(10000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        int status = connection.getResponseCode();
        String response = readResponse(connection, status);
        if (status < 200 || status >= 300) {
            throw new IOException("server returned HTTP " + status + ": " + response);
        }
        return response;
    }

    private String readResponse(HttpURLConnection connection, int status) throws IOException {
        InputStream input = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (input == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
