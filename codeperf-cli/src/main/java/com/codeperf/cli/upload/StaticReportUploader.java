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

public class StaticReportUploader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String upload(String serverUrl, StaticReportUploadRequest request) throws IOException {
        String taskId = createTask(serverUrl, request);
        post(serverUrl + "/api/tasks/" + taskId + "/static-results", request.getReportJson());
        return taskId;
    }

    private String createTask(String serverUrl, StaticReportUploadRequest request) throws IOException {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("project", request.getProject());
        payload.put("commit", request.getCommit());
        payload.put("branch", request.getBranch());
        payload.put("env", request.getEnv());
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
