package com.cmb.codeperf.server.service.impl;

import com.cmb.codeperf.server.model.bo.AnalysisTaskBO;
import com.cmb.codeperf.server.model.bo.DynamicEvidenceBO;
import com.cmb.codeperf.server.model.bo.FindingIssueBO;
import com.cmb.codeperf.server.model.dto.response.RiskAttributionSummary;
import com.cmb.codeperf.server.model.dto.response.StaticFindingSummary;
import com.cmb.codeperf.server.model.dto.response.StaticReportSummary;
import com.cmb.codeperf.server.model.vo.report.BranchReportIssueVO;
import com.cmb.codeperf.server.model.vo.report.BranchReportOwnerGroupVO;
import com.cmb.codeperf.server.model.vo.report.BranchReportPageVO;
import com.cmb.codeperf.server.model.vo.report.ReportDetailPageVO;
import com.cmb.codeperf.server.model.vo.report.ReportDynamicEvidenceVO;
import com.cmb.codeperf.server.model.vo.report.ReportFindingCardVO;
import com.cmb.codeperf.server.model.vo.report.ReportListItemVO;
import com.cmb.codeperf.server.model.vo.report.ReportListPageVO;
import com.cmb.codeperf.server.model.vo.report.RuntimeCorroborationSummaryVO;
import com.cmb.codeperf.server.service.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告页面数据组装服务。
 * 页面层需要把静态 finding 和动态运行证据放在同一张卡片里对齐，避免用户只能看到两份彼此孤立的数据。
 */
@Service
public class ReportPageService {

    private static final int DEFAULT_TASK_LIMIT = 50;

    private final AnalysisTaskRepository repository;
    private final StaticReportSummarizer staticReportSummarizer;
    private final RuntimeEvidenceAggregator runtimeEvidenceAggregator;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportPageService(AnalysisTaskRepository repository,
                             StaticReportSummarizer staticReportSummarizer,
                             RuntimeEvidenceAggregator runtimeEvidenceAggregator) {
        this.repository = repository;
        this.staticReportSummarizer = staticReportSummarizer;
        this.runtimeEvidenceAggregator = runtimeEvidenceAggregator;
    }

    /**
     * 构建报告列表页。
     *
     * @return 最近任务列表页模型
     */
    public ReportListPageVO getListPage() {
        List<AnalysisTaskBO> tasks = repository.listRecentTasks(DEFAULT_TASK_LIMIT);
        ReportListPageVO page = new ReportListPageVO();
        List<ReportListItemVO> items = new ArrayList<>(tasks.size());
        for (AnalysisTaskBO task : tasks) {
            items.add(toListItem(task));
        }
        page.setTasks(items);
        page.setTaskCount(items.size());
        return page;
    }

    /**
     * 构建单任务综合报告页。
     *
     * @param taskId 分析任务ID
     * @return 报告详情页模型
     */
    public ReportDetailPageVO getDetailPage(String taskId) {
        AnalysisTaskBO task = repository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("analysis task not found: " + taskId));
        List<DynamicEvidenceBO> dynamicRecords = repository.listDynamicEvidence(taskId);
        ReportDetailPageVO page = new ReportDetailPageVO();
        fillTaskInfo(page, task);
        fillStaticSummary(page, task, dynamicRecords);
        fillDynamicEvidence(page, dynamicRecords);
        page.setConclusion(buildConclusion(page));
        return page;
    }

    /**
     * 构建分支综合报告页。
     *
     * @param remoteUrl 远程仓库地址
     * @param branch 分支名称
     * @param env 环境名称
     * @return 分支报告页模型
     */
    public BranchReportPageVO getBranchPage(String remoteUrl, String branch, String env) {
        List<FindingIssueBO> issues = repository.listOpenIssues(remoteUrl, branch);
        List<DynamicEvidenceBO> dynamicRecords = repository.listLatestDynamicEvidence(remoteUrl, branch, env);
        RuntimeEvidenceIndex runtimeEvidenceIndex = buildRuntimeEvidenceIndex(dynamicRecords);
        BranchReportPageVO page = new BranchReportPageVO();
        page.setRemoteUrl(remoteUrl);
        page.setBranch(branch);
        page.setEnv(env);
        page.setOpenIssueCount(issues.size());
        page.setLatestDynamicTaskId(dynamicRecords.isEmpty() ? "" : dynamicRecords.get(0).getTaskId());
        Map<String, BranchReportOwnerGroupVO> groups = new LinkedHashMap<>();
        for (FindingIssueBO issue : issues) {
            BranchReportOwnerGroupVO group = groups.computeIfAbsent(ownerKey(issue), ignored -> newOwnerGroup(issue));
            BranchReportIssueVO issueVO = toBranchIssue(issue, runtimeEvidenceIndex);
            group.getIssues().add(issueVO);
            group.setIssueCount(group.getIssueCount() + 1);
            if ("已运行命中".equals(issueVO.getRuntimeStatus())) {
                group.setCorroboratedCount(group.getCorroboratedCount() + 1);
                page.setCorroboratedIssueCount(page.getCorroboratedIssueCount() + 1);
            }
        }
        page.setOwnerGroups(new ArrayList<>(groups.values()));
        page.setOwnerCount(page.getOwnerGroups().size());
        return page;
    }

    private ReportListItemVO toListItem(AnalysisTaskBO task) {
        ReportListItemVO item = new ReportListItemVO();
        item.setTaskId(task.getAnalysisTaskId());
        item.setProjectName(task.getProject());
        item.setBranch(task.getBranch());
        item.setCommit(shortCommit(task.getCommit()));
        item.setEnv(task.getEnv());
        item.setCreatedAt(task.getCreatedAt());
        item.setUpdatedAt(task.getUpdatedAt());
        item.setStatus(task.getStatus().name());
        item.setRiskLevel(task.getRiskLevel().name());
        item.setStaticRiskLevel(task.getStaticRiskLevel().name());
        item.setHasStaticResult(hasText(task.getStaticPayload()));
        item.setHasDynamicEvidence(hasText(task.getDynamicPayload()));
        item.setAuthorName(task.getAuthorName());
        item.setAuthorEmail(task.getAuthorEmail());
        item.setCommitMessage(task.getCommitMessage());
        return item;
    }

    private BranchReportOwnerGroupVO newOwnerGroup(FindingIssueBO issue) {
        BranchReportOwnerGroupVO group = new BranchReportOwnerGroupVO();
        group.setOwnerName(valueOrDefault(issue.getOwnerName(), "未识别开发者"));
        group.setOwnerEmail(valueOrDefault(issue.getOwnerEmail(), "UNKNOWN"));
        return group;
    }

    private BranchReportIssueVO toBranchIssue(FindingIssueBO issue, RuntimeEvidenceIndex runtimeEvidenceIndex) {
        BranchReportIssueVO vo = new BranchReportIssueVO();
        vo.setSourceFile(issue.getSourceFile());
        vo.setFileName(fileName(issue.getSourceFile()));
        vo.setLineNumber(issue.getLineNumber());
        vo.setRuleId(issue.getRuleId());
        vo.setSeverity(issue.getSeverity());
        vo.setLoopMethodName(issue.getLoopMethodName());
        vo.setIoType(issue.getIoType());
        vo.setRawPayload(issue.getRawPayload());
        RuntimeCorroboration corroboration = runtimeEvidenceIndex.match(issue.getLoopMethodName(), issue.getRawPayload());
        if (corroboration == null) {
            vo.setRuntimeStatus("未运行命中");
            return vo;
        }
        vo.setRuntimeStatus("已运行命中");
        vo.setRuntimeEntryKey(corroboration.getEntryKey());
        vo.setRuntimeCallPath(corroboration.getCallPath());
        vo.setRuntimeRepeatCount(corroboration.getRepeatCount());
        return vo;
    }

    private String ownerKey(FindingIssueBO issue) {
        return valueOrDefault(issue.getOwnerEmail(), "UNKNOWN");
    }

    private void fillTaskInfo(ReportDetailPageVO page, AnalysisTaskBO task) {
        page.setTaskId(task.getAnalysisTaskId());
        page.setProjectName(task.getProject());
        page.setRemoteUrl(task.getRemoteUrl());
        page.setCommit(task.getCommit());
        page.setBranch(task.getBranch());
        page.setEnv(task.getEnv());
        page.setAuthorName(task.getAuthorName());
        page.setAuthorEmail(task.getAuthorEmail());
        page.setAuthorTime(task.getAuthorTime());
        page.setCommitMessage(task.getCommitMessage());
        page.setStatus(task.getStatus().name());
        page.setRiskLevel(task.getRiskLevel().name());
        page.setStaticRiskLevel(task.getStaticRiskLevel().name());
    }

    private void fillStaticSummary(ReportDetailPageVO page, AnalysisTaskBO task, List<DynamicEvidenceBO> dynamicRecords) {
        StaticReportSummary summary = staticReportSummarizer.summarize(task.getStaticPayload());
        if (summary == null) {
            return;
        }
        page.setFilesScanned(summary.getFilesScanned());
        page.setFindingCount(summary.getFindingCount());
        page.setParseErrorCount(summary.getParseErrorCount());
        Map<String, RuntimeCorroborationSummaryVO> runtimeSummaries =
                runtimeEvidenceAggregator.aggregate(summary.getFindings(), dynamicRecords);
        List<ReportFindingCardVO> cards = new ArrayList<>(summary.getFindings().size());
        for (StaticFindingSummary finding : summary.getFindings()) {
            cards.add(toFindingCard(finding, runtimeSummaries.get(runtimeEvidenceAggregator.findingKey(finding))));
        }
        page.setFindingCards(cards);
        fillRuntimeSummary(page, cards);
    }

    private ReportFindingCardVO toFindingCard(StaticFindingSummary finding, RuntimeCorroborationSummaryVO runtimeSummary) {
        ReportFindingCardVO card = new ReportFindingCardVO();
        card.setRuleId(finding.getRuleId());
        card.setSeverity(finding.getSeverity());
        card.setConfidence(finding.getConfidence());
        card.setSourceFile(finding.getSourceFile());
        card.setEvidence(finding.getEvidence());
        card.setFileName(fileName(finding.getSourceFile()));
        card.setLineNumber(finding.getLineNumber());
        card.setLoopStartLine(finding.getLoopStartLine());
        card.setLoopEndLine(finding.getLoopEndLine());
        card.setLoopCallLine(finding.getLoopCallLine());
        card.setIoLine(finding.getIoLine());
        card.setMethodName(finding.getLoopMethodName());
        card.setIoType(finding.getIoType());
        card.setLocation(card.getFileName() + ":" + finding.getLineNumber());
        fillAttribution(card, finding.getAttribution());
        fillRuntimeCorroboration(card, runtimeSummary);
        return card;
    }

    private void fillRuntimeSummary(ReportDetailPageVO page, List<ReportFindingCardVO> cards) {
        int corroboratedCount = 0;
        int highAmplificationCount = 0;
        for (ReportFindingCardVO card : cards) {
            if (!"未命中".equals(card.getRuntimeCorroborationStatus())) {
                corroboratedCount++;
            }
            if ("高放大命中".equals(card.getRuntimeCorroborationStatus())) {
                highAmplificationCount++;
            }
        }
        page.setRuntimeCorroboratedFindingCount(corroboratedCount);
        page.setRuntimeHighAmplificationCount(highAmplificationCount);
    }

    private void fillAttribution(ReportFindingCardVO card, RiskAttributionSummary attribution) {
        if (attribution == null) {
            card.setRiskScope("UNKNOWN");
            return;
        }
        card.setRiskScope(valueOrDefault(attribution.getRiskScope(), "UNKNOWN"));
        card.setIntroducedByName(attribution.getIntroducedByName());
        card.setIntroducedByEmail(attribution.getIntroducedByEmail());
        card.setIntroducedCommit(shortCommit(attribution.getIntroducedCommit()));
    }

    private void fillRuntimeCorroboration(ReportFindingCardVO card, RuntimeCorroborationSummaryVO summary) {
        if (summary == null || !summary.isMatched()) {
            card.setRuntimeCorroborationStatus("未命中");
            card.setRuntimeCorroborationText("当前没有找到能够直接对应这条静态风险的运行证据。");
            return;
        }
        card.setRuntimeCorroborationStatus(runtimeStatusText(summary.getStatus()));
        card.setRuntimeCorroborationText(summary.getText());
        card.setRuntimeEntryKey(summary.getLatestEntryKey());
        card.setRuntimeCallPath(summary.getLatestCallPath());
        card.setRuntimeMatchedMethod(summary.getLatestMatchedMethod());
        card.setRuntimeRepeatCount(summary.getMaxRepeatCount());
        card.setRuntimeMatchedReason(summary.getMatchedReason());
        card.setRuntimeHitRequestCount(summary.getHitRequestCount());
        card.setRuntimeHitEntryCount(summary.getHitEntryCount());
        card.setRuntimeAvgRepeatCount(summary.getAvgRepeatCount());
        card.setRuntimeTopEntryKey(summary.getTopEntryKey());
        card.setRuntimeTopEntries(summary.getTopEntries());
    }

    private String runtimeStatusText(String status) {
        if ("HIGH_AMPLIFICATION".equals(status)) {
            return "高放大命中";
        }
        if ("MULTI_ENTRY_HIT".equals(status)) {
            return "多入口命中";
        }
        if ("FREQUENT_HIT".equals(status)) {
            return "高频命中";
        }
        if ("HIT".equals(status)) {
            return "已命中";
        }
        return "未命中";
    }

    private void fillDynamicEvidence(ReportDetailPageVO page, List<DynamicEvidenceBO> records) {
        List<ReportDynamicEvidenceVO> evidenceList = new ArrayList<>(records.size());
        for (DynamicEvidenceBO record : records) {
            ReportDynamicEvidenceVO evidence = new ReportDynamicEvidenceVO();
            evidence.setAppName(record.getAppName());
            evidence.setEnv(record.getEnv());
            evidence.setEntryKey(record.getEntryKey());
            evidence.setRawPayload(record.getRawPayload());
            evidenceList.add(evidence);
        }
        page.setDynamicEvidenceList(evidenceList);
    }

    private RuntimeEvidenceIndex buildRuntimeEvidenceIndex(List<DynamicEvidenceBO> dynamicRecords) {
        List<RuntimeRequestSnapshot> requests = new ArrayList<>();
        for (DynamicEvidenceBO record : dynamicRecords) {
            requests.addAll(parseRequests(record));
        }
        return new RuntimeEvidenceIndex(requests);
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
        return hasText(entryKey) ? entryKey : fallback;
    }

    private void collectRuntimeCalls(JsonNode node, List<String> path, List<RuntimeCallSnapshot> calls) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        String method = text(node, "method");
        if (!hasText(method) || "ROOT".equals(method)) {
            JsonNode children = node.path("children");
            if (children.isArray()) {
                for (JsonNode child : children) {
                    collectRuntimeCalls(child, path, calls);
                }
            }
            return;
        }
        List<String> nextPath = new ArrayList<>(path);
        nextPath.add(simpleMethodDisplay(method));
        calls.add(new RuntimeCallSnapshot(method, simpleMethodDisplay(method), joinPath(nextPath), node.path("count").asInt(0)));
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                collectRuntimeCalls(child, nextPath, calls);
            }
        }
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

    private String buildConclusion(ReportDetailPageVO page) {
        if (page.getFindingCount() == 0) {
            return "本次任务未发现静态结构风险；动态运行证据可作为补充验证，不改变当前静态结论。";
        }
        if (!page.getDynamicEvidenceList().isEmpty()) {
            return "动态运行证据已聚合到静态风险卡片，重点查看命中入口、命中请求数、最大/平均重复调用和最近运行路径。";
        }
        return "当前仅有静态结构风险，尚未收到动态运行证据；建议优先修复本次变更风险，再补充运行验证。";
    }

    private String fileName(String sourceFile) {
        if (sourceFile == null || sourceFile.trim().isEmpty()) {
            return "UNKNOWN";
        }
        String normalized = sourceFile.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String shortCommit(String commit) {
        if (commit == null) {
            return "";
        }
        String value = commit.trim();
        return value.length() > 8 ? value.substring(0, 8) : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private static final class RuntimeEvidenceIndex {
        private final List<RuntimeRequestSnapshot> requests;

        private RuntimeEvidenceIndex(List<RuntimeRequestSnapshot> requests) {
            this.requests = requests;
        }

        private RuntimeCorroboration match(StaticFindingSummary finding) {
            List<String> candidates = buildCandidates(finding);
            return matchCandidates(candidates);
        }

        private RuntimeCorroboration match(String methodName, String evidence) {
            List<String> candidates = new ArrayList<>();
            candidates.add(extractMethodName(evidence));
            candidates.add(methodName);
            return matchCandidates(candidates);
        }

        private RuntimeCorroboration matchCandidates(List<String> candidates) {
            for (String candidate : candidates) {
                if (!hasText(candidate)) {
                    continue;
                }
                RuntimeCallSnapshot best = null;
                String bestEntry = "";
                for (RuntimeRequestSnapshot request : requests) {
                    RuntimeCallSnapshot matched = request.findByMethodName(candidate);
                    if (matched != null && (best == null || matched.getCount() > best.getCount())) {
                        best = matched;
                        bestEntry = request.getEntryKey();
                    }
                }
                if (best != null) {
                    return new RuntimeCorroboration(bestEntry, best.getCallPath(), best.getSimpleMethodName(),
                            best.getCount(), candidate);
                }
            }
            return null;
        }

        private List<String> buildCandidates(StaticFindingSummary finding) {
            List<String> candidates = new ArrayList<>();
            if (finding == null) {
                return candidates;
            }
            String evidenceMethod = extractMethodName(finding.getEvidence());
            if (hasText(evidenceMethod)) {
                candidates.add(evidenceMethod);
            }
            if (hasText(finding.getLoopMethodName())) {
                candidates.add(finding.getLoopMethodName());
            }
            return candidates;
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
    }

    private static final class RuntimeRequestSnapshot {
        private final String entryKey;
        private final List<RuntimeCallSnapshot> calls;

        private RuntimeRequestSnapshot(String entryKey, List<RuntimeCallSnapshot> calls) {
            this.entryKey = entryKey;
            this.calls = calls;
        }

        private String getEntryKey() {
            return entryKey;
        }

        private RuntimeCallSnapshot findByMethodName(String methodName) {
            RuntimeCallSnapshot best = null;
            for (RuntimeCallSnapshot call : calls) {
                if (!methodName.equals(call.getMethodName())) {
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

    private static final class RuntimeCorroboration {
        private final String entryKey;
        private final String callPath;
        private final String matchedMethod;
        private final int repeatCount;
        private final String matchedReason;

        private RuntimeCorroboration(String entryKey, String callPath, String matchedMethod, int repeatCount,
                                     String matchedReason) {
            this.entryKey = entryKey;
            this.callPath = callPath;
            this.matchedMethod = matchedMethod;
            this.repeatCount = repeatCount;
            this.matchedReason = matchedReason;
        }

        private String getText() {
            StringBuilder builder = new StringBuilder();
            builder.append("入口 ").append(entryKey);
            builder.append(" 下，运行路径命中 ").append(callPath);
            builder.append("，重复调用 ").append(repeatCount).append(" 次。");
            builder.append(" 匹配依据：").append(matchedReason);
            return builder.toString();
        }

        private String getEntryKey() {
            return entryKey;
        }

        private String getCallPath() {
            return callPath;
        }

        private String getMatchedMethod() {
            return matchedMethod;
        }

        private int getRepeatCount() {
            return repeatCount;
        }

        private String getMatchedReason() {
            return matchedReason;
        }
    }
}
