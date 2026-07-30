package com.cmb.codeperf.server.service.impl;

import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.dto.response.StaticFindingSummary;
import com.cmb.codeperf.server.model.vo.report.RuntimeCorroborationEntryVO;
import com.cmb.codeperf.server.model.vo.report.RuntimeCorroborationSummaryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态证据聚合器。
 * 将多次 agent 上报的原始请求调用树聚合到每条静态风险上，避免页面只展示单次最大命中而丢失频次和入口分布。
 */
@Component
public class RuntimeEvidenceAggregator {

    private static final String STATUS_NOT_HIT = "NOT_HIT";
    private static final String STATUS_HIT = "HIT";
    private static final String STATUS_FREQUENT_HIT = "FREQUENT_HIT";
    private static final String STATUS_MULTI_ENTRY_HIT = "MULTI_ENTRY_HIT";
    private static final String STATUS_HIGH_AMPLIFICATION = "HIGH_AMPLIFICATION";
    private static final int FREQUENT_HIT_THRESHOLD = 5;
    private static final int HIGH_AMPLIFICATION_THRESHOLD = 20;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 按静态风险聚合动态佐证。
     *
     * @param findings 静态风险列表
     * @param dynamicRecords 动态证据记录
     * @return key为findingKey的动态佐证摘要
     */
    public Map<String, RuntimeCorroborationSummaryVO> aggregate(List<StaticFindingSummary> findings,
                                                                List<DynamicEvidenceBO> dynamicRecords) {
        List<RuntimeRequestSnapshot> requests = parseRequests(dynamicRecords);
        Map<String, RuntimeCorroborationSummaryVO> result = new LinkedHashMap<>();
        if (findings == null) {
            return result;
        }
        for (StaticFindingSummary finding : findings) {
            result.put(findingKey(finding), summarize(finding, requests));
        }
        return result;
    }

    /**
     * 生成稳定的静态风险聚合键。
     *
     * @param finding 静态风险
     * @return 聚合键
     */
    public String findingKey(StaticFindingSummary finding) {
        if (finding == null) {
            return "";
        }
        return value(finding.getRuleId())
                + "|" + value(finding.getSourceFile())
                + "|" + finding.getLineNumber()
                + "|" + value(finding.getEvidence());
    }

    private RuntimeCorroborationSummaryVO summarize(StaticFindingSummary finding, List<RuntimeRequestSnapshot> requests) {
        List<String> candidates = buildCandidates(finding);
        List<RuntimeHitSnapshot> hits = new ArrayList<>();
        for (RuntimeRequestSnapshot request : requests) {
            RuntimeCallSnapshot bestCall = request.findBestByCandidates(candidates);
            if (bestCall != null) {
                hits.add(new RuntimeHitSnapshot(request, bestCall, matchedReason(candidates, bestCall)));
            }
        }
        if (hits.isEmpty()) {
            return notHit();
        }
        return hitSummary(hits);
    }

    private RuntimeCorroborationSummaryVO notHit() {
        RuntimeCorroborationSummaryVO summary = new RuntimeCorroborationSummaryVO();
        summary.setStatus(STATUS_NOT_HIT);
        summary.setMatched(false);
        summary.setText("当前没有找到能够直接对应这条静态风险的运行证据。");
        return summary;
    }

    private RuntimeCorroborationSummaryVO hitSummary(List<RuntimeHitSnapshot> hits) {
        RuntimeCorroborationSummaryVO summary = new RuntimeCorroborationSummaryVO();
        RuntimeHitSnapshot latest = hits.get(hits.size() - 1);
        Map<String, EntryStats> entryStats = buildEntryStats(hits);
        EntryStats topEntry = topEntry(entryStats);
        int maxRepeat = 0;
        int totalRepeat = 0;
        for (RuntimeHitSnapshot hit : hits) {
            maxRepeat = Math.max(maxRepeat, hit.call.getCount());
            totalRepeat += hit.call.getCount();
        }
        summary.setMatched(true);
        summary.setHitRequestCount(hits.size());
        summary.setHitEntryCount(entryStats.size());
        summary.setMaxRepeatCount(maxRepeat);
        summary.setAvgRepeatCount(totalRepeat / hits.size());
        summary.setTopEntryKey(topEntry == null ? "" : topEntry.entryKey);
        summary.setLatestEntryKey(latest.request.entryKey);
        summary.setLatestCallPath(latest.call.getCallPath());
        summary.setLatestMatchedMethod(latest.call.getSimpleMethodName());
        summary.setMatchedReason(latest.matchedReason);
        summary.setStatus(status(summary));
        summary.setTopEntries(toEntryVOs(entryStats));
        summary.setText("动态运行已命中：命中请求 " + summary.getHitRequestCount()
                + " 次，涉及入口 " + summary.getHitEntryCount()
                + " 个，最大重复调用 " + summary.getMaxRepeatCount()
                + " 次，平均重复调用 " + summary.getAvgRepeatCount()
                + " 次。最近入口：" + summary.getLatestEntryKey());
        return summary;
    }

    private String status(RuntimeCorroborationSummaryVO summary) {
        if (summary.getMaxRepeatCount() >= HIGH_AMPLIFICATION_THRESHOLD) {
            return STATUS_HIGH_AMPLIFICATION;
        }
        if (summary.getHitEntryCount() >= 2) {
            return STATUS_MULTI_ENTRY_HIT;
        }
        if (summary.getHitRequestCount() >= FREQUENT_HIT_THRESHOLD) {
            return STATUS_FREQUENT_HIT;
        }
        return STATUS_HIT;
    }

    private Map<String, EntryStats> buildEntryStats(List<RuntimeHitSnapshot> hits) {
        Map<String, EntryStats> stats = new LinkedHashMap<>();
        for (RuntimeHitSnapshot hit : hits) {
            EntryStats stat = stats.get(hit.request.entryKey);
            if (stat == null) {
                stat = new EntryStats(hit.request.entryKey);
                stats.put(hit.request.entryKey, stat);
            }
            stat.hitRequestCount++;
            stat.maxRepeatCount = Math.max(stat.maxRepeatCount, hit.call.getCount());
            stat.totalRepeatCount += hit.call.getCount();
            stat.latestCallPath = hit.call.getCallPath();
        }
        return stats;
    }

    private EntryStats topEntry(Map<String, EntryStats> entryStats) {
        EntryStats top = null;
        for (EntryStats current : entryStats.values()) {
            if (top == null
                    || current.maxRepeatCount > top.maxRepeatCount
                    || (current.maxRepeatCount == top.maxRepeatCount
                    && current.hitRequestCount > top.hitRequestCount)) {
                top = current;
            }
        }
        return top;
    }

    private List<RuntimeCorroborationEntryVO> toEntryVOs(Map<String, EntryStats> entryStats) {
        List<EntryStats> values = new ArrayList<>(entryStats.values());
        Collections.sort(values, new Comparator<EntryStats>() {
            @Override
            public int compare(EntryStats left, EntryStats right) {
                int repeatCompare = Integer.compare(right.maxRepeatCount, left.maxRepeatCount);
                if (repeatCompare != 0) {
                    return repeatCompare;
                }
                return Integer.compare(right.hitRequestCount, left.hitRequestCount);
            }
        });
        List<RuntimeCorroborationEntryVO> result = new ArrayList<>(values.size());
        for (EntryStats stat : values) {
            RuntimeCorroborationEntryVO vo = new RuntimeCorroborationEntryVO();
            vo.setEntryKey(stat.entryKey);
            vo.setHitRequestCount(stat.hitRequestCount);
            vo.setMaxRepeatCount(stat.maxRepeatCount);
            vo.setAvgRepeatCount(stat.totalRepeatCount / stat.hitRequestCount);
            vo.setLatestCallPath(stat.latestCallPath);
            result.add(vo);
        }
        return result;
    }

    private List<RuntimeRequestSnapshot> parseRequests(List<DynamicEvidenceBO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<RuntimeRequestSnapshot> snapshots = new ArrayList<>();
        for (DynamicEvidenceBO record : records) {
            snapshots.addAll(parseRequests(record));
        }
        return snapshots;
    }

    private List<RuntimeRequestSnapshot> parseRequests(DynamicEvidenceBO record) {
        if (record == null || !hasText(record.getRawPayload())) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = mapper.readTree(record.getRawPayload());
            JsonNode evidence = root.path("evidence");
            JsonNode requests = evidence.path("requests");
            if (!requests.isArray()) {
                return Collections.emptyList();
            }
            List<RuntimeRequestSnapshot> snapshots = new ArrayList<>();
            for (JsonNode request : requests) {
                String entryKey = requestEntryKey(request, record.getEntryKey());
                List<RuntimeCallSnapshot> calls = new ArrayList<>();
                collectRuntimeCalls(request.path("callTree"), new ArrayList<String>(), calls);
                snapshots.add(new RuntimeRequestSnapshot(entryKey, calls));
            }
            return snapshots;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private String requestEntryKey(JsonNode request, String fallback) {
        String method = text(request, "httpMethod");
        String path = text(request, "path");
        String entryKey = (method + " " + path).trim();
        return hasText(entryKey) ? entryKey : value(fallback);
    }

    private void collectRuntimeCalls(JsonNode node, List<String> path, List<RuntimeCallSnapshot> calls) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        String method = text(node, "method");
        if (!hasText(method) || "ROOT".equals(method)) {
            collectChildren(node, path, calls);
            return;
        }
        List<String> nextPath = new ArrayList<>(path);
        nextPath.add(simpleMethodDisplay(method));
        calls.add(new RuntimeCallSnapshot(method, simpleMethodDisplay(method), joinPath(nextPath), node.path("count").asInt(0)));
        collectChildren(node, nextPath, calls);
    }

    private void collectChildren(JsonNode node, List<String> path, List<RuntimeCallSnapshot> calls) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            collectRuntimeCalls(child, path, calls);
        }
    }

    private List<String> buildCandidates(StaticFindingSummary finding) {
        List<String> candidates = new ArrayList<>();
        if (finding == null) {
            return candidates;
        }
        String evidenceMethod = extractMethodName(finding.getEvidence());
        if (hasText(evidenceMethod)) {
            candidates.add(evidenceMethod);
        } else if (hasText(finding.getLoopMethodName())) {
            candidates.add(finding.getLoopMethodName());
        }
        return candidates;
    }

    private String matchedReason(List<String> candidates, RuntimeCallSnapshot call) {
        for (String candidate : candidates) {
            if (call.matches(candidate)) {
                return candidate;
            }
        }
        return call.getSimpleMethodName();
    }

    private String extractMethodName(String evidence) {
        if (!hasText(evidence)) {
            return "";
        }
        String trimmed = evidence.trim();
        int open = trimmed.lastIndexOf('(');
        if (open <= 0) {
            return "";
        }
        int end = open - 1;
        while (end >= 0 && Character.isWhitespace(trimmed.charAt(end))) {
            end--;
        }
        int start = end;
        while (start >= 0) {
            char current = trimmed.charAt(start);
            if (Character.isLetterOrDigit(current) || current == '_' || current == '$' || current == '.') {
                start--;
                continue;
            }
            break;
        }
        String token = trimmed.substring(start + 1, end + 1);
        int dot = token.lastIndexOf('.');
        return dot >= 0 ? token.substring(dot + 1) : token;
    }

    private String simpleMethodDisplay(String fullMethod) {
        if (!hasText(fullMethod)) {
            return "";
        }
        int lastDot = fullMethod.lastIndexOf('.');
        if (lastDot < 0) {
            return fullMethod;
        }
        int classStart = fullMethod.lastIndexOf('.', lastDot - 1);
        String className = classStart >= 0 ? fullMethod.substring(classStart + 1, lastDot) : fullMethod.substring(0, lastDot);
        return className + "." + fullMethod.substring(lastDot + 1);
    }

    private String joinPath(List<String> path) {
        if (path.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(path.get(i));
        }
        return builder.toString();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class EntryStats {
        private final String entryKey;
        private int hitRequestCount;
        private int maxRepeatCount;
        private int totalRepeatCount;
        private String latestCallPath;

        private EntryStats(String entryKey) {
            this.entryKey = entryKey;
        }
    }

    private static final class RuntimeHitSnapshot {
        private final RuntimeRequestSnapshot request;
        private final RuntimeCallSnapshot call;
        private final String matchedReason;

        private RuntimeHitSnapshot(RuntimeRequestSnapshot request, RuntimeCallSnapshot call, String matchedReason) {
            this.request = request;
            this.call = call;
            this.matchedReason = matchedReason;
        }
    }

    private static final class RuntimeRequestSnapshot {
        private final String entryKey;
        private final List<RuntimeCallSnapshot> calls;

        private RuntimeRequestSnapshot(String entryKey, List<RuntimeCallSnapshot> calls) {
            this.entryKey = entryKey;
            this.calls = calls;
        }

        private RuntimeCallSnapshot findBestByCandidates(List<String> candidates) {
            RuntimeCallSnapshot best = null;
            for (RuntimeCallSnapshot call : calls) {
                if (!call.matchesAny(candidates)) {
                    continue;
                }
                if (best == null || call.getCount() > best.getCount()) {
                    best = call;
                }
            }
            return best;
        }
    }

    private static final class RuntimeCallSnapshot {
        private final String fullMethodName;
        private final String methodName;
        private final String callPath;
        private final int count;

        private RuntimeCallSnapshot(String fullMethodName, String methodName, String callPath, int count) {
            this.fullMethodName = fullMethodName;
            this.methodName = methodName;
            this.callPath = callPath;
            this.count = count;
        }

        private boolean matchesAny(List<String> candidates) {
            for (String candidate : candidates) {
                if (matches(candidate)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matches(String candidate) {
            if (candidate == null || candidate.trim().isEmpty()) {
                return false;
            }
            String value = candidate.trim();
            return value.equals(getMethodName()) || fullMethodName.endsWith("." + value);
        }

        private String getMethodName() {
            int lastDot = methodName.lastIndexOf('.');
            return lastDot >= 0 ? methodName.substring(lastDot + 1) : methodName;
        }

        private String getSimpleMethodName() {
            return methodName;
        }

        private String getCallPath() {
            return callPath;
        }

        private int getCount() {
            return count;
        }
    }
}
