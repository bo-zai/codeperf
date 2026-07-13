package com.codeperf.cli.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CodePerf Server HTTP 客户端。
 * CLI 只通过该类创建任务、上传静态结果、读取门禁结论，避免命令层直接拼接 HTTP 细节。
 */
public class CodePerfServerClient {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public CodePerfServerClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 创建一次分析任务，由 CI 或 Git 流程在静态/动态证据上传前调用。
     *
     * @param project 项目名称
     * @param commit 提交哈希
     * @param branch 分支名
     * @param env 环境标识
     * @return 服务端生成的 analysis_task_id
     * @throws IOException 服务端不可达或响应异常时抛出
     */
    public String createTask(String project, String commit, String branch, String env) throws IOException {
        Map<String, String> request = new LinkedHashMap<>();
        request.put("project", project);
        request.put("commit", commit);
        request.put("branch", branch);
        request.put("env", env);
        String response = post("/api/tasks", objectMapper.writeValueAsString(request));
        JsonNode json = objectMapper.readTree(response);
        return json.path("analysisTaskId").asText();
    }

    /**
     * 上传静态扫描结果。结果内容保持 CLI 生成的 JSON 原貌，服务端负责解析与归档。
     *
     * @param taskId 分析任务 ID
     * @param payload 静态扫描 JSON
     * @throws IOException 服务端不可达或响应异常时抛出
     */
    public void uploadStaticResult(String taskId, String payload) throws IOException {
        post("/api/tasks/" + taskId + "/static-results", payload);
    }

    /**
     * 查询服务端门禁结论，供 CI/pre-push 根据风险等级决定是否失败。
     *
     * @param taskId 分析任务 ID
     * @return 门禁结论
     * @throws IOException 服务端不可达或响应异常时抛出
     */
    public GateResult getGate(String taskId) throws IOException {
        String response = get("/api/tasks/" + taskId + "/gate");
        return objectMapper.readValue(response, GateResult.class);
    }

    private String post(String path, String body) throws IOException {
        HttpURLConnection connection = open(path, "POST");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
        return readResponse(connection);
    }

    private String get(String path) throws IOException {
        HttpURLConnection connection = open(path, "GET");
        return readResponse(connection);
    }

    private HttpURLConnection open(String path, String method) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", CONTENT_TYPE_JSON);
        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream input = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = input == null ? "" : readBody(input);
        if (status < 200 || status >= 300) {
            throw new IOException("CodePerf Server 响应异常，status=" + status + ", body=" + body);
        }
        return body;
    }

    private String readBody(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        StringBuilder builder = new StringBuilder();
        try (InputStream in = input) {
            while ((read = in.read(buffer)) >= 0) {
                builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
        }
        return builder.toString();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("server url must not be blank");
        }
        String value = url.trim();
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
