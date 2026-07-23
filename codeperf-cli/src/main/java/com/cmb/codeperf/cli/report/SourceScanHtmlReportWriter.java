package com.cmb.codeperf.cli.report;

import com.cmb.codeperf.analysis.source.CallChainStep;
import com.cmb.codeperf.analysis.source.RiskAttribution;
import com.cmb.codeperf.analysis.source.SourceFinding;
import com.cmb.codeperf.analysis.source.SourceScanResult;
import com.cmb.codeperf.cli.config.ModuleScanConfig;
import com.cmb.codeperf.cli.module.SourceModuleResolver;
import com.cmb.codeperf.cli.report.SourceScanHtmlSupport.IndexedFinding;
import com.cmb.codeperf.cli.report.SourceScanHtmlSupport.SourceLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.escape;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.escapeAttribute;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.fileAnchor;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.isBlank;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.location;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.now;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.readSourceSnippet;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.shortFileName;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.shortLocation;
import static com.cmb.codeperf.cli.report.SourceScanHtmlSupport.valueOrUnknown;

/**
 * HTML 报告生成器：生成离线可用的源码风险诊断报告。
 */
public class SourceScanHtmlReportWriter {

    private static final String RULE_TITLE = "循环内外部 I/O 调用可能被生产数据量放大";

    /**
     * 将源码扫描结果写入 HTML 文件。
     *
     * @param output HTML 输出路径
     * @param result 源码扫描结果
     * @throws IOException 文件写入失败时抛出
     */
    public void write(Path output, SourceScanResult result) throws IOException {
        write(output, result, Paths.get("."));
    }

    /**
     * 将源码扫描结果写入 HTML 文件，并从项目根目录读取源码片段。
     *
     * @param output HTML 输出路径
     * @param result 源码扫描结果
     * @param projectRoot 项目根目录
     * @throws IOException 文件写入失败时抛出
     */
    public void write(Path output, SourceScanResult result, Path projectRoot) throws IOException {
        write(output, result, projectRoot, Collections.<ModuleScanConfig>emptyList());
    }

    /**
     * 将源码扫描结果写入 HTML 文件，并支持模块维度展示。
     *
     * @param output HTML 输出路径
     * @param result 源码扫描结果
     * @param projectRoot 项目根目录
     * @param modules 模块配置
     * @throws IOException 文件写入失败时抛出
     */
    public void write(Path output, SourceScanResult result, Path projectRoot, List<ModuleScanConfig> modules)
            throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.write(output, render(result, projectRoot, modules).getBytes(StandardCharsets.UTF_8));
    }

    private String render(SourceScanResult result, Path projectRoot, List<ModuleScanConfig> modules) {
        List<SourceFinding> findings = sortedFindings(result);
        SourceModuleResolver moduleResolver = new SourceModuleResolver(modules);
        StringBuilder html = new StringBuilder(32768);
        appendDocumentStart(html);
        appendTopBar(html, result, findings, moduleResolver);
        appendFilterToolbar(html, findings, moduleResolver);
        appendIssueWorkspace(html, findings, projectRoot, moduleResolver);
        appendFloatingToc(html, findings, moduleResolver);
        appendParseErrors(html, result);
        appendScript(html);
        html.append("</div></body></html>\n");
        return html.toString();
    }

    private void appendDocumentStart(StringBuilder html) {
        html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"zh-CN\">\n")
                .append("<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("<title>CodePerf 本地代码扫描报告</title>\n")
                .append("<style>").append(SourceScanHtmlAssets.style()).append("</style>\n")
                .append("</head>\n")
                .append("<body><div class=\"report-page\">\n");
    }

    private void appendTopBar(StringBuilder html, SourceScanResult result, List<SourceFinding> findings,
            SourceModuleResolver moduleResolver) {
        boolean failed = !findings.isEmpty();
        html.append("<header class=\"topbar\">")
                .append("<div><div class=\"topbar-title\">CodePerf 本地代码扫描报告</div>")
                .append("<div class=\"topbar-subtitle\">生成时间 ").append(escape(now()))
                .append(" · 面向提交前修复的循环 I/O 放大风险诊断</div></div>")
                .append("<div class=\"quality-gate ").append(failed ? "failed" : "passed").append("\">")
                .append("<span>质量门禁</span>").append(failed ? "检测失败" : "检测通过").append("</div>")
                .append("<div class=\"summary-metrics\">");
        appendMetric(html, "阻断风险", String.valueOf(findings.size()));
        appendMetric(html, "风险总数", String.valueOf(findings.size()));
        appendMetric(html, "扫描文件", String.valueOf(result.getFilesScanned()));
        appendMetric(html, "模块数", String.valueOf(countModules(findings, moduleResolver)));
        appendMetric(html, "解析错误", String.valueOf(result.getParseErrors().size()));
        html.append("</div></header>\n");
    }

    private void appendMetric(StringBuilder html, String label, String value) {
        html.append("<div class=\"metric\"><span class=\"metric-label\">").append(escape(label))
                .append("</span><b class=\"metric-value\">").append(escape(value)).append("</b></div>");
    }

    private void appendFilterToolbar(StringBuilder html, List<SourceFinding> findings, SourceModuleResolver moduleResolver) {
        html.append("<section class=\"filter-toolbar\" aria-label=\"风险过滤器\">")
                .append("<div class=\"toolbar-title\">过滤问题</div>");
        appendTextFilter(html);
        appendSelectStart(html, "filterModule", "模块", "全部模块");
        for (String module : moduleNames(findings, moduleResolver)) {
            html.append("<option value=\"").append(escapeAttribute(module)).append("\">")
                    .append(escape(module)).append("</option>");
        }
        html.append("</select></div>");
        appendFixedSelect(html, "filterScope", "归因范围", "全部归因",
                new String[][]{{"NEW", "本次新增"}, {"MODIFIED", "本次修改"}, {"HISTORICAL", "历史风险"}, {"UNKNOWN", "未归因"}});
        appendFixedSelect(html, "filterIo", "I/O 类型", "全部 I/O",
                new String[][]{{"DB", "DB"}, {"Redis", "Redis"}, {"MongoDB", "MongoDB"}, {"GaussDB", "GaussDB"},
                        {"HTTP", "HTTP"}, {"RPC", "RPC"}, {"SDK", "SDK"}});
        appendFixedSelect(html, "filterSeverity", "严重级别", "全部级别",
                new String[][]{{"CRITICAL", "CRITICAL"}, {"WARN", "WARN"}, {"INFO", "INFO"}});
        appendFixedSelect(html, "filterConfidence", "置信度", "全部置信度",
                new String[][]{{"HIGH", "HIGH"}, {"MEDIUM", "MEDIUM"}, {"LOW", "LOW"}});
        html.append("</section>");
    }

    private void appendTextFilter(StringBuilder html) {
        html.append("<div class=\"filter-group\"><label class=\"filter-label\" for=\"filterText\">搜索</label>")
                .append("<input id=\"filterText\" type=\"search\" placeholder=\"文件、方法、证据、提交人\"></div>");
    }

    private void appendSelectStart(StringBuilder html, String id, String label, String emptyLabel) {
        html.append("<div class=\"filter-group\"><label class=\"filter-label\" for=\"").append(id).append("\">")
                .append(escape(label)).append("</label><select id=\"").append(id).append("\"><option value=\"\">")
                .append(escape(emptyLabel)).append("</option>");
    }

    private void appendFixedSelect(StringBuilder html, String id, String label, String emptyLabel, String[][] options) {
        appendSelectStart(html, id, label, emptyLabel);
        for (String[] option : options) {
            html.append("<option value=\"").append(escapeAttribute(option[0])).append("\">")
                    .append(escape(option[1])).append("</option>");
        }
        html.append("</select></div>");
    }

    private void appendIssueWorkspace(StringBuilder html, List<SourceFinding> findings, Path projectRoot,
            SourceModuleResolver moduleResolver) {
        html.append("<main class=\"issue-feed\"><div class=\"workspace-head\"><div>")
                .append("<h2>问题列表</h2><p>按模块和文件聚合，点击问题行在当前位置展开诊断详情。</p>")
                .append("</div><span class=\"module-count\">").append(findings.size()).append(" 个问题</span></div>");
        if (findings.isEmpty()) {
            html.append("<div class=\"empty-state\">未发现静态结构风险。</div></main>");
            return;
        }
        Map<String, Map<String, List<IndexedFinding>>> groups = groupedByModule(findings, moduleResolver);
        for (Map.Entry<String, Map<String, List<IndexedFinding>>> moduleEntry : groups.entrySet()) {
            appendModuleBlock(html, moduleEntry.getKey(), moduleEntry.getValue(), projectRoot, moduleResolver);
        }
        html.append("<div id=\"emptyResult\" class=\"empty-state hidden\">未匹配到风险，请调整过滤条件。</div>")
                .append("</main>");
    }

    private void appendModuleBlock(StringBuilder html, String moduleName, Map<String, List<IndexedFinding>> fileGroups,
            Path projectRoot, SourceModuleResolver moduleResolver) {
        html.append("<section class=\"module-block\" data-module-block>")
                .append("<div class=\"module-head\"><span>").append(escape(moduleName)).append("</span>")
                .append("<span class=\"module-count\">").append(countFindings(fileGroups)).append(" 个问题</span></div>");
        for (Map.Entry<String, List<IndexedFinding>> fileEntry : fileGroups.entrySet()) {
            appendFileCard(html, moduleName, fileEntry.getKey(), fileEntry.getValue(), projectRoot, moduleResolver);
        }
        html.append("</section>");
    }

    private void appendFileCard(StringBuilder html, String moduleName, String sourceFile, List<IndexedFinding> findings,
            Path projectRoot, SourceModuleResolver moduleResolver) {
        html.append("<section id=\"").append(escapeAttribute(fileAnchor(moduleName + "/" + sourceFile)))
                .append("\" class=\"file-card\" data-file-card>")
                .append("<div class=\"file-head\"><div><div class=\"file-name\">")
                .append(escape(shortFileName(sourceFile))).append("</div><div class=\"file-path\">")
                .append(escape(sourceFile)).append("</div></div><div class=\"file-count\">")
                .append(findings.size()).append(" 个问题</div></div>")
                .append("<div class=\"chips\">")
                .append(statChip("最高级别", highestSeverity(findings), severityTone(highestSeverity(findings))))
                .append(summaryChips(fileConfidenceCounts(findings), "blue"))
                .append(summaryChips(fileIoCounts(findings), "orange"))
                .append("</div><div class=\"issue-list\">");
        for (IndexedFinding finding : findings) {
            appendIssueRow(html, moduleName, finding);
            appendDetailCard(html, finding, projectRoot, moduleResolver);
        }
        html.append("</div></section>");
    }

    private void appendIssueRow(StringBuilder html, String moduleName, IndexedFinding indexedFinding) {
        SourceFinding finding = indexedFinding.finding;
        RiskAttribution attribution = attributionOrUnknown(finding);
        html.append("<button id=\"issue-").append(indexedFinding.index).append("\" class=\"issue-row")
                .append(indexedFinding.index == 0 ? " active" : "")
                .append("\" data-issue-row data-target=\"detail-").append(indexedFinding.index).append("\"")
                .append(" data-module=\"").append(escapeAttribute(moduleName)).append("\"")
                .append(" data-io=\"").append(escapeAttribute(finding.getIoType())).append("\"")
                .append(" data-severity=\"").append(escapeAttribute(finding.getSeverity().name())).append("\"")
                .append(" data-confidence=\"").append(escapeAttribute(finding.getConfidence().name())).append("\"")
                .append(" data-scope=\"").append(escapeAttribute(attribution.getRiskScope().name())).append("\"")
                .append(" data-search=\"").append(escapeAttribute(searchText(finding, moduleName))).append("\">")
                .append("<span class=\"issue-line\">L").append(finding.getLineNumber()).append("</span>")
                .append("<span><span class=\"issue-title\">").append(RULE_TITLE).append("</span>")
                .append("<span class=\"issue-meta\">")
                .append(chip(finding.getRuleId(), "blue"))
                .append(chip(finding.getSeverity().name(), severityTone(finding.getSeverity().name())))
                .append(chip(finding.getConfidence().name(), "blue"))
                .append(chip(finding.getIoType(), "orange"))
                .append(scopeChip(attribution))
                .append("</span><span class=\"issue-evidence\">").append(escape(finding.getEvidence()))
                .append("</span></span>")
                .append("<span class=\"issue-method\">").append(escape(finding.getLoopMethodName())).append("</span>")
                .append("</button>");
    }

    private void appendDetailCard(StringBuilder html, IndexedFinding indexedFinding, Path projectRoot,
            SourceModuleResolver moduleResolver) {
        SourceFinding finding = indexedFinding.finding;
        String moduleName = moduleResolver.resolveModuleName(finding.getSourceFile());
        html.append("<article id=\"detail-").append(indexedFinding.index).append("\" class=\"detail-card")
                .append(indexedFinding.index == 0 ? " active" : "")
                .append("\" data-detail>")
                .append("<div class=\"detail-location\">").append(escape(shortLocation(finding))).append("</div>")
                .append("<div class=\"chips\">")
                .append(chip(moduleName, "blue"))
                .append(chip(finding.getRuleId(), "blue"))
                .append(chip(finding.getSeverity().name(), severityTone(finding.getSeverity().name())))
                .append(chip(finding.getConfidence().name(), "blue"))
                .append(chip(finding.getIoType(), "orange"))
                .append(scopeChip(attributionOrUnknown(finding)))
                .append("</div>");
        appendRiskExplanation(html);
        appendEvidence(html, finding);
        appendSourceSnippet(html, finding, projectRoot);
        appendCallChain(html, finding);
        appendLoopMeta(html, finding);
        appendAttribution(html, attributionOrUnknown(finding));
        appendSuggestions(html, finding);
        html.append("</article>");
    }

    private void appendFloatingToc(StringBuilder html, List<SourceFinding> findings, SourceModuleResolver moduleResolver) {
        html.append("<nav class=\"floating-toc\" aria-label=\"报告目录\"><div class=\"toc-title\">目录</div>");
        if (findings.isEmpty()) {
            html.append("<div class=\"toc-empty\">暂无风险</div></nav>");
            return;
        }
        Map<String, Map<String, List<IndexedFinding>>> groups = groupedByModule(findings, moduleResolver);
        for (Map.Entry<String, Map<String, List<IndexedFinding>>> moduleEntry : groups.entrySet()) {
            html.append("<div class=\"toc-module\">").append(escape(moduleEntry.getKey()))
                    .append(" · ").append(countFindings(moduleEntry.getValue())).append("</div>");
            for (Map.Entry<String, List<IndexedFinding>> fileEntry : moduleEntry.getValue().entrySet()) {
                html.append("<a class=\"toc-file\" href=\"#")
                        .append(escapeAttribute(fileAnchor(moduleEntry.getKey() + "/" + fileEntry.getKey()))).append("\">")
                        .append(escape(shortFileName(fileEntry.getKey()))).append(" · ")
                        .append(fileEntry.getValue().size()).append("</a>");
                for (IndexedFinding finding : fileEntry.getValue()) {
                    html.append("<a class=\"toc-issue\" href=\"#issue-").append(finding.index).append("\">L")
                            .append(finding.finding.getLineNumber()).append(" · ")
                            .append(escape(finding.finding.getIoType())).append("</a>");
                }
            }
        }
        html.append("</nav>");
    }

    private void appendRiskExplanation(StringBuilder html) {
        html.append("<section class=\"detail-section\"><h3>风险解释</h3>")
                .append("<div class=\"detail-text\">循环内触发外部 I/O 时，测试环境的小数据量可能只产生少量访问；")
                .append("生产数据规模放大后会变成 N 次数据库、缓存、HTTP、RPC 或 SDK 调用，导致接口响应时间被线性放大。</div></section>");
    }

    private void appendEvidence(StringBuilder html, SourceFinding finding) {
        html.append("<section class=\"detail-section\"><h3>证据</h3><div class=\"evidence-box\">")
                .append(escape(finding.getEvidence())).append("</div></section>");
    }

    private void appendSourceSnippet(StringBuilder html, SourceFinding finding, Path projectRoot) {
        html.append("<section class=\"detail-section\"><h3>源码片段</h3>");
        List<SourceLine> lines = readSourceSnippet(finding, projectRoot);
        if (lines.isEmpty()) {
            html.append("<div class=\"empty-state\">源码片段不可用，请根据文件路径和行号在 IDE 中查看。</div></section>");
            return;
        }
        html.append("<pre class=\"source-block\">");
        for (SourceLine line : lines) {
            html.append("<span class=\"source-line");
            if (line.getLineNumber() == finding.getLineNumber()) {
                html.append(" hit");
            }
            html.append("\"><span class=\"line-no\">").append(line.getLineNumber()).append("</span>")
                    .append(escape(line.getContent())).append("</span>");
        }
        html.append("</pre></section>");
    }

    private void appendCallChain(StringBuilder html, SourceFinding finding) {
        html.append("<section class=\"detail-section\"><h3>调用链</h3>");
        if (finding.getCallChain() == null || finding.getCallChain().isEmpty()) {
            html.append("<div class=\"detail-text\">直接在循环体内触发</div></section>");
            return;
        }
        html.append("<div class=\"call-chain\">");
        for (int i = 0; i < finding.getCallChain().size(); i++) {
            CallChainStep step = finding.getCallChain().get(i);
            if (i > 0) {
                html.append("<span>→</span>");
            }
            html.append("<span class=\"chain-step\">").append(escape(step.getClassName())).append(".")
                    .append(escape(step.getMethodName())).append(":").append(step.getLineNumber()).append("</span>");
        }
        html.append("</div></section>");
    }

    private void appendLoopMeta(StringBuilder html, SourceFinding finding) {
        html.append("<section class=\"detail-section\"><h3>循环范围</h3><div class=\"meta-grid\">");
        appendMeta(html, "源码文件", finding.getSourceFile());
        appendMeta(html, "循环方法", finding.getLoopMethodName());
        appendMeta(html, "循环范围", finding.getLoopStartLine() + "-" + finding.getLoopEndLine());
        appendMeta(html, "循环调用行", String.valueOf(finding.getLoopCallLine()));
        appendMeta(html, "I/O 行", String.valueOf(finding.getIoLine()));
        appendMeta(html, "I/O 类型", finding.getIoType());
        html.append("</div></section>");
    }

    private void appendAttribution(StringBuilder html, RiskAttribution attribution) {
        html.append("<section class=\"detail-section\"><h3>归因信息</h3>");
        if (RiskAttribution.RiskScope.UNKNOWN.equals(attribution.getRiskScope()) || isBlank(attribution.getIntroducedByName())) {
            html.append("<div class=\"detail-text\">本次扫描未获得增量归因。通常出现在全量扫描、缺少 diff 基线或无法 blame 的场景。</div></section>");
            return;
        }
        html.append("<div class=\"meta-grid\">");
        appendMeta(html, "提交人", attribution.getIntroducedByName());
        appendMeta(html, "邮箱", attribution.getIntroducedByEmail());
        appendMeta(html, "Commit", attribution.getIntroducedCommit());
        appendMeta(html, "提交时间", attribution.getIntroducedCommitTime());
        html.append("</div>");
        if (!isBlank(attribution.getIntroducedCommitMessage())) {
            html.append("<div class=\"evidence-box\" style=\"margin-top:10px\">")
                    .append(escape(attribution.getIntroducedCommitMessage())).append("</div>");
        }
        html.append("</section>");
    }

    private void appendSuggestions(StringBuilder html, SourceFinding finding) {
        html.append("<section class=\"detail-section\"><h3>修复建议</h3><ul class=\"suggestions\">")
                .append("<li>优先将循环内的单条 I/O 改为批量查询或批量接口调用。</li>")
                .append("<li>在循环外预加载数据，并用 Map 按 ID 回填循环内结果。</li>");
        if ("DB".equalsIgnoreCase(finding.getIoType())) {
            html.append("<li>数据库访问建议使用 IN 查询、批量 Mapper 方法或一次性分页加载。</li>");
        } else if ("SDK".equalsIgnoreCase(finding.getIoType()) || "HTTP".equalsIgnoreCase(finding.getIoType())
                || "RPC".equalsIgnoreCase(finding.getIoType())) {
            html.append("<li>外部服务访问建议合并 RPC/HTTP 请求，或增加本地缓存与降级策略。</li>");
        }
        html.append("<li>修复后重新执行 codeperf scan，确认该风险不再出现在报告中。</li></ul></section>");
    }

    private void appendMeta(StringBuilder html, String label, String value) {
        html.append("<div class=\"meta-item\">").append(escape(label)).append("<b>")
                .append(escape(valueOrUnknown(value))).append("</b></div>");
    }

    private void appendParseErrors(StringBuilder html, SourceScanResult result) {
        html.append("<section class=\"parse-errors\"><h2>解析错误</h2>");
        if (result.getParseErrors().isEmpty()) {
            html.append("<div class=\"detail-text\">无解析错误。</div></section>");
            return;
        }
        html.append("<div class=\"detail-text\">解析错误不会阻止报告生成，但会影响对应文件的检测完整性。</div>");
        for (String parseError : result.getParseErrors()) {
            html.append("<div class=\"parse-item\">").append(escape(parseError)).append("</div>");
        }
        html.append("</section>");
    }

    private void appendScript(StringBuilder html) {
        html.append("<script>").append(SourceScanHtmlAssets.script()).append("</script>\n");
    }

    private List<SourceFinding> sortedFindings(SourceScanResult result) {
        List<SourceFinding> findings = new ArrayList<>(result.getFindings());
        final Map<String, Integer> fileCounts = fileCounts(findings);
        Collections.sort(findings, new Comparator<SourceFinding>() {
            @Override
            public int compare(SourceFinding left, SourceFinding right) {
                int scope = Integer.compare(scopeScore(right), scopeScore(left));
                if (scope != 0) {
                    return scope;
                }
                int severity = Integer.compare(right.getSeverity().getLevel(), left.getSeverity().getLevel());
                if (severity != 0) {
                    return severity;
                }
                int confidence = Integer.compare(confidenceScore(right), confidenceScore(left));
                if (confidence != 0) {
                    return confidence;
                }
                int io = Integer.compare(ioScore(right), ioScore(left));
                if (io != 0) {
                    return io;
                }
                int fileCount = Integer.compare(fileCounts.get(right.getSourceFile()), fileCounts.get(left.getSourceFile()));
                if (fileCount != 0) {
                    return fileCount;
                }
                return location(left).compareTo(location(right));
            }
        });
        return findings;
    }

    private Map<String, Map<String, List<IndexedFinding>>> groupedByModule(List<SourceFinding> findings,
            SourceModuleResolver moduleResolver) {
        Map<String, Map<String, List<IndexedFinding>>> groups = new LinkedHashMap<>();
        for (int i = 0; i < findings.size(); i++) {
            SourceFinding finding = findings.get(i);
            String moduleName = moduleResolver.resolveModuleName(finding.getSourceFile());
            if (!groups.containsKey(moduleName)) {
                groups.put(moduleName, new LinkedHashMap<String, List<IndexedFinding>>());
            }
            Map<String, List<IndexedFinding>> fileGroups = groups.get(moduleName);
            if (!fileGroups.containsKey(finding.getSourceFile())) {
                fileGroups.put(finding.getSourceFile(), new ArrayList<IndexedFinding>());
            }
            fileGroups.get(finding.getSourceFile()).add(new IndexedFinding(i, finding));
        }
        return groups;
    }

    private List<String> moduleNames(List<SourceFinding> findings, SourceModuleResolver moduleResolver) {
        List<String> modules = new ArrayList<>();
        for (SourceFinding finding : findings) {
            String module = moduleResolver.resolveModuleName(finding.getSourceFile());
            if (!modules.contains(module)) {
                modules.add(module);
            }
        }
        return modules;
    }

    private int countModules(List<SourceFinding> findings, SourceModuleResolver moduleResolver) {
        return moduleNames(findings, moduleResolver).size();
    }

    private int countFindings(Map<String, List<IndexedFinding>> groups) {
        int count = 0;
        for (List<IndexedFinding> findings : groups.values()) {
            count += findings.size();
        }
        return count;
    }

    private Map<String, Integer> fileCounts(List<SourceFinding> findings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SourceFinding finding : findings) {
            Integer count = counts.get(finding.getSourceFile());
            counts.put(finding.getSourceFile(), count == null ? 1 : count + 1);
        }
        return counts;
    }

    private Map<String, Integer> fileConfidenceCounts(List<IndexedFinding> findings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (IndexedFinding indexedFinding : findings) {
            addCount(counts, indexedFinding.finding.getConfidence().name());
        }
        return counts;
    }

    private Map<String, Integer> fileIoCounts(List<IndexedFinding> findings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (IndexedFinding indexedFinding : findings) {
            addCount(counts, indexedFinding.finding.getIoType());
        }
        return counts;
    }

    private void addCount(Map<String, Integer> counts, String key) {
        Integer count = counts.get(key);
        counts.put(key, count == null ? 1 : count + 1);
    }

    private String highestSeverity(List<IndexedFinding> findings) {
        int best = 0;
        String severity = "INFO";
        for (IndexedFinding indexedFinding : findings) {
            if (indexedFinding.finding.getSeverity().getLevel() > best) {
                best = indexedFinding.finding.getSeverity().getLevel();
                severity = indexedFinding.finding.getSeverity().name();
            }
        }
        return severity;
    }

    private String summaryChips(Map<String, Integer> counts, String tone) {
        StringBuilder html = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            html.append(statChip(entry.getKey(), String.valueOf(entry.getValue()), tone));
        }
        return html.toString();
    }

    private String statChip(String label, String value, String tone) {
        return "<span class=\"chip " + escapeAttribute(tone) + "\">" + escape(label) + " " + escape(value) + "</span>";
    }

    private String chip(String value, String tone) {
        return "<span class=\"chip " + escapeAttribute(tone) + "\">" + escape(value) + "</span>";
    }

    private String scopeChip(RiskAttribution attribution) {
        RiskAttribution.RiskScope scope = attribution.getRiskScope();
        if (RiskAttribution.RiskScope.NEW.equals(scope) || RiskAttribution.RiskScope.MODIFIED.equals(scope)) {
            return chip("本次变更", "red");
        }
        if (RiskAttribution.RiskScope.HISTORICAL.equals(scope)) {
            return chip("历史风险", "orange");
        }
        return chip("未归因", "");
    }

    private String severityTone(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity) || "WARN".equalsIgnoreCase(severity)) {
            return "red";
        }
        if ("INFO".equalsIgnoreCase(severity)) {
            return "green";
        }
        return "orange";
    }

    private int scopeScore(SourceFinding finding) {
        RiskAttribution.RiskScope scope = attributionOrUnknown(finding).getRiskScope();
        if (RiskAttribution.RiskScope.NEW.equals(scope) || RiskAttribution.RiskScope.MODIFIED.equals(scope)) {
            return 3;
        }
        if (RiskAttribution.RiskScope.HISTORICAL.equals(scope)) {
            return 2;
        }
        return 1;
    }

    private int confidenceScore(SourceFinding finding) {
        if (SourceFinding.Confidence.HIGH.equals(finding.getConfidence())) {
            return 3;
        }
        if (SourceFinding.Confidence.MEDIUM.equals(finding.getConfidence())) {
            return 2;
        }
        return 1;
    }

    private int ioScore(SourceFinding finding) {
        String ioType = finding.getIoType();
        if ("DB".equalsIgnoreCase(ioType) || "HTTP".equalsIgnoreCase(ioType) || "RPC".equalsIgnoreCase(ioType)) {
            return 3;
        }
        if ("SDK".equalsIgnoreCase(ioType)) {
            return 2;
        }
        return 1;
    }

    private RiskAttribution attributionOrUnknown(SourceFinding finding) {
        if (finding.getAttribution() == null) {
            return RiskAttribution.unknown();
        }
        return finding.getAttribution();
    }

    private String searchText(SourceFinding finding, String moduleName) {
        RiskAttribution attribution = attributionOrUnknown(finding);
        return moduleName + " " + finding.getSourceFile() + " " + finding.getLoopMethodName() + " "
                + finding.getEvidence() + " " + finding.getDescription() + " " + finding.getIoType() + " "
                + finding.getRuleId() + " " + valueOrUnknown(attribution.getIntroducedByName()) + " "
                + valueOrUnknown(attribution.getIntroducedByEmail());
    }

}

