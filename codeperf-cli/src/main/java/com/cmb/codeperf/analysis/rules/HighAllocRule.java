package com.cmb.codeperf.analysis.rules;

import com.cmb.codeperf.analysis.Finding;
import com.cmb.codeperf.analysis.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测高内存分配。逐请求检查 allocBytes。
 * 见 docs/04-analysis-report.md 第 3.4 节。
 */
public class HighAllocRule implements AnalysisRule {

    private static final long MB = 1024 * 1024;

    @Override
    public List<Finding> analyze(JsonNode session) {
        List<Finding> findings = new ArrayList<>();
        JsonNode requests = session.get("requests");
        if (requests == null || !requests.isArray()) return findings;

        for (JsonNode req : requests) {
            long allocBytes = req.path("allocBytes").asLong(0);
            if (allocBytes <= 0) continue;

            Severity severity;
            if (allocBytes >= 500 * MB) {
                severity = Severity.CRITICAL;
            } else if (allocBytes >= 100 * MB) {
                severity = Severity.WARN;
            } else if (allocBytes >= 50 * MB) {
                severity = Severity.INFO;
            } else {
                continue;
            }

            double allocMB = allocBytes / (double) MB;
            String path = req.path("path").asText("");
            long wallMs = req.path("wallTimeMs").asLong(0);

            String desc = String.format("高内存分配: 请求 %s 分配了 %.1f MB", path, allocMB);
            String evidence = String.format("请求: %s | 分配: %.1f MB | 耗时: %dms | 线程: %s",
                    path, allocMB, wallMs, req.path("threadName").asText(""));
            findings.add(new Finding("High Allocation", severity, desc, evidence));
        }
        return findings;
    }
}

