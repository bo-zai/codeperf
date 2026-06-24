package com.codeperf.analysis.rules;

import com.codeperf.analysis.Finding;
import com.codeperf.analysis.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检测 CPU 热点方法。汇总所有请求的栈采样，按栈顶帧聚合占比。
 * 见 docs/04-analysis-report.md 第 3.3 节。
 */
public class CpuHotspotRule implements AnalysisRule {

    @Override
    public List<Finding> analyze(JsonNode session) {
        List<Finding> findings = new ArrayList<>();
        JsonNode requests = session.get("requests");
        if (requests == null || !requests.isArray()) return findings;

        // 汇总所有采样帧
        Map<String, Integer> topFrameCounts = new HashMap<>();
        int totalSamples = 0;

        for (JsonNode req : requests) {
            JsonNode samples = req.get("samples");
            if (samples == null || !samples.isArray()) continue;

            for (JsonNode sample : samples) {
                JsonNode frames = sample.get("frames");
                if (frames == null || !frames.isArray() || frames.size() == 0) continue;

                totalSamples++;
                String topFrame = frames.get(0).asText("");
                topFrameCounts.put(topFrame, topFrameCounts.getOrDefault(topFrame, 0) + 1);
            }
        }

        if (totalSamples < 10) return findings;

        // 按占比排序，输出热点
        for (Map.Entry<String, Integer> entry : topFrameCounts.entrySet()) {
            double pct = 100.0 * entry.getValue() / totalSamples;
            Severity severity;
            if (pct >= 30.0 && totalSamples >= 20) {
                severity = Severity.WARN;
            } else if (pct >= 10.0) {
                severity = Severity.INFO;
            } else {
                continue;
            }

            String method = entry.getKey();
            String desc = String.format("CPU 热点: %s 占用 %.1f%% 采样", method, pct);
            String evidence = String.format("方法: %s | 采样数: %d/%d | 占比: %.1f%%", method, entry.getValue(), totalSamples, pct);
            findings.add(new Finding("CPU Hotspot", severity, desc, evidence));
        }
        return findings;
    }
}
