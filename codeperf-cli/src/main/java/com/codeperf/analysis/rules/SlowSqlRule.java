package com.codeperf.analysis.rules;

import com.codeperf.analysis.Finding;
import com.codeperf.analysis.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测慢 SQL。依赖 agent 的 slow 标记 + maxMs 分级。
 * 见 docs/04-analysis-report.md 第 3.2 节。
 */
public class SlowSqlRule implements AnalysisRule {

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
                if (!sql.path("slow").asBoolean(false)) continue;

                long maxMs = sql.path("maxMs").asLong(0);
                String fingerprint = sql.path("fingerprint").asText("?");
                String sample = sql.path("sampleSql").asText("");
                int count = sql.path("count").asInt(0);

                Severity severity;
                if (maxMs >= 1000) {
                    severity = Severity.CRITICAL;
                } else if (maxMs >= 500) {
                    severity = Severity.WARN;
                } else {
                    severity = Severity.INFO;
                }

                String desc = String.format("慢 SQL: %s 最大耗时 %dms", fingerprint, maxMs);
                String evidence = String.format("请求: %s | SQL: %s | 最大耗时: %dms | 执行次数: %d", path, sample, maxMs, count);
                findings.add(new Finding("Slow SQL", severity, desc, evidence));
            }
        }
        return findings;
    }
}
