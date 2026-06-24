package com.codeperf.analysis.rules;

import com.codeperf.analysis.Finding;
import com.codeperf.analysis.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测高延迟请求。逐请求检查 wallTimeMs。
 * 见 docs/04-analysis-report.md 第 3.5 节。
 */
public class HighLatencyRule implements AnalysisRule {

    @Override
    public List<Finding> analyze(JsonNode session) {
        List<Finding> findings = new ArrayList<>();
        JsonNode requests = session.get("requests");
        if (requests == null || !requests.isArray()) return findings;

        for (JsonNode req : requests) {
            long wallMs = req.path("wallTimeMs").asLong(0);
            if (wallMs <= 0) continue;

            Severity severity;
            if (wallMs >= 5000) {
                severity = Severity.CRITICAL;
            } else if (wallMs >= 2000) {
                severity = Severity.WARN;
            } else if (wallMs >= 1000) {
                severity = Severity.INFO;
            } else {
                continue;
            }

            String path = req.path("path").asText("");
            int status = req.path("status").asInt(0);

            String desc = String.format("高延迟: %s 耗时 %dms", path, wallMs);
            String evidence = String.format("请求: %s | 耗时: %dms | HTTP 状态: %d | 线程: %s",
                    path, wallMs, status, req.path("threadName").asText(""));
            findings.add(new Finding("High Latency", severity, desc, evidence));
        }
        return findings;
    }
}
