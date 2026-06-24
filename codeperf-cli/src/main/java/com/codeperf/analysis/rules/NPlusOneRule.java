package com.codeperf.analysis.rules;

import com.codeperf.analysis.Finding;
import com.codeperf.analysis.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测 N+1 查询模式。逐请求遍历 sqls，按 count 判定严重度。
 * 见 docs/04-analysis-report.md 第 3.1 节。
 */
public class NPlusOneRule implements AnalysisRule {

    @Override
    public List<Finding> analyze(JsonNode session) {
        List<Finding> findings = new ArrayList<>();
        JsonNode requests = session.get("requests");
        if (requests == null || !requests.isArray()) return findings;

        for (JsonNode req : requests) {
            String path = req.path("path").asText("");
            JsonNode sqls = req.get("sqls");
            if (sqls == null || !sqls.isArray()) continue;

            for (JsonNode sql : sqls) {
                int count = sql.path("count").asInt(0);
                if (count <= 2) continue;

                String fingerprint = sql.path("fingerprint").asText("?");
                String sample = sql.path("sampleSql").asText("");
                long totalMs = sql.path("totalMs").asLong(0);

                Severity severity;
                if (count >= 50) {
                    severity = Severity.CRITICAL;
                } else if (count >= 10) {
                    severity = Severity.WARN;
                } else {
                    severity = Severity.INFO;
                }

                String desc = String.format("N+1 查询: %s 在单请求内执行了 %d 次", fingerprint, count);
                String evidence = String.format("请求: %s | SQL: %s | 次数: %d | 总耗时: %dms", path, sample, count, totalMs);
                findings.add(new Finding("N+1 Query", severity, desc, evidence));
            }
        }
        return findings;
    }
}
