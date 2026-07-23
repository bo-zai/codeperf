package com.cmb.codeperf.analysis;

import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.cmb.codeperf.analysis.staticanalysis.StaticResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * HTML 报告生成器：根据动态分析或静态分析结果生成自包含 HTML 报告。
 * <p>
 * 特性：
 * <ul>
 *   <li>单文件输出：样式内嵌，离线可用</li>
 *   <li>动态报告：包含调用树、SQL 汇总、CPU 热点等</li>
 *   <li>静态报告：包含方法位置、置信度、证据等</li>
 * </ul>
 * <p>
 * 注意：此报告主要用于动态分析场景，源码扫描使用 SourceScanHtmlReportWriter。
 * <p>
 * 见 docs/04-analysis-report.md 第 5 节。
 */
public class HtmlReport {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String generate(JsonNode session, List<Finding> findings) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"UTF-8\">");
        h.append("<title>CodePerf 性能报告</title>");
        h.append("<style>");
        h.append("*{margin:0;padding:0;box-sizing:border-box;}");
        h.append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#333;background:#f5f5f5;}");
        h.append(".header{background:#2c3e50;color:#fff;padding:24px 32px;}");
        h.append(".header h1{font-size:22px;margin-bottom:8px;}");
        h.append(".header .meta{font-size:13px;opacity:0.8;line-height:1.6;}");
        h.append(".container{max-width:1100px;margin:0 auto;padding:24px;}");
        h.append(".card{background:#fff;border-radius:6px;box-shadow:0 1px 3px rgba(0,0,0,.1);padding:20px 24px;margin-bottom:20px;}");
        h.append(".card h2{font-size:17px;margin-bottom:14px;padding-bottom:8px;border-bottom:2px solid #eee;}");
        h.append("table{width:100%;border-collapse:collapse;font-size:13px;}");
        h.append("th{text-align:left;padding:8px 10px;background:#f8f9fa;border-bottom:2px solid #dee2e6;font-weight:600;}");
        h.append("td{padding:7px 10px;border-bottom:1px solid #e9ecef;}");
        h.append(".badge{display:inline-block;padding:2px 8px;border-radius:3px;font-size:11px;font-weight:700;color:#fff;}");
        h.append(".badge-crit{background:#e74c3c;} .badge-warn{background:#f39c12;} .badge-info{background:#3498db;}");
        h.append(".badge-none{background:#27ae60;}");
        h.append(".finding{margin-bottom:12px;padding:12px 16px;border-left:4px solid #ddd;background:#fafafa;}");
        h.append(".finding.crit{border-left-color:#e74c3c;} .finding.warn{border-left-color:#f39c12;} .finding.info{border-left-color:#3498db;}");
        h.append(".finding .type{font-weight:600;margin-bottom:4px;}");
        h.append(".finding .desc{font-size:13px;color:#555;margin-bottom:4px;}");
        h.append(".finding .evid{font-size:12px;color:#888;font-family:monospace;}");
        h.append(".call-tree{font-family:'SF Mono',Consolas,monospace;font-size:12px;line-height:1.7;white-space:pre;}");
        h.append(".summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;}");
        h.append(".summary-item{text-align:center;padding:16px;background:#fff;border-radius:6px;box-shadow:0 1px 3px rgba(0,0,0,.1);}");
        h.append(".summary-item .num{font-size:28px;font-weight:700;}");
        h.append(".summary-item .label{font-size:12px;color:#888;margin-top:4px;}");
        h.append(".summary-item.crit .num{color:#e74c3c;} .summary-item.warn .num{color:#f39c12;} .summary-item.info .num{color:#3498db;} .summary-item.ok .num{color:#27ae60;}");
        h.append("</style></head><body>");

        // ---- Header ----
        String entry = session.path("entryMethod").asText("") + " " + session.path("entryPath").asText("");
        long startMs = session.path("startTimeEpochMs").asLong(0);
        String timeStr = startMs > 0 ? SDF.format(new Date(startMs)) : "N/A";
        String javaVer = session.path("javaVersion").asText("N/A");
        int reqCount = session.get("requests") != null ? session.get("requests").size() : 0;

        h.append("<div class=\"header\">");
        h.append("<h1>CodePerf 性能分析报告</h1>");
        h.append("<div class=\"meta\">");
        h.append("入口: ").append(escape(entry)).append(" &nbsp;|&nbsp; ");
        h.append("采集时间: ").append(timeStr).append(" &nbsp;|&nbsp; ");
        h.append("Java: ").append(escape(javaVer)).append(" &nbsp;|&nbsp; ");
        h.append("请求数: ").append(reqCount);
        h.append("</div></div>");

        h.append("<div class=\"container\">");

        // ---- Summary ----
        long critCount = findings.stream().filter(f -> f.getSeverity() == Severity.CRITICAL).count();
        long warnCount = findings.stream().filter(f -> f.getSeverity() == Severity.WARN).count();
        long infoCount = findings.stream().filter(f -> f.getSeverity() == Severity.INFO).count();
        Severity maxSev = findings.isEmpty() ? null :
                findings.stream().map(Finding::getSeverity).max(Comparator.comparingInt(Severity::getLevel)).get();

        h.append("<div class=\"summary-grid\">");
        summaryItem(h, "crit", (int) critCount, "严重 (Critical)");
        summaryItem(h, "warn", (int) warnCount, "警告 (Warning)");
        summaryItem(h, "info", (int) infoCount, "提示 (Info)");
        String verdict = maxSev == null ? "通过" : "发现问题 (max: " + maxSev.name() + ")";
        String verdictClass = maxSev == null ? "ok" : maxSev.name().toLowerCase();
        h.append("<div class=\"summary-item ").append(verdictClass).append("\">");
        h.append("<div class=\"num\">").append(maxSev == null ? "OK" : "!").append("</div>");
        h.append("<div class=\"label\">").append(verdict).append("</div></div>");
        h.append("</div>");

        // ---- Findings ----
        if (!findings.isEmpty()) {
            h.append("<div class=\"card\"><h2>问题列表</h2>");
            List<Finding> sorted = findings.stream()
                    .sorted(Comparator.comparingInt((Finding f) -> f.getSeverity().getLevel()).reversed())
                    .collect(Collectors.toList());
            for (Finding f : sorted) {
                String cssClass = sevClass(f.getSeverity());
                h.append("<div class=\"finding ").append(cssClass).append("\">");
                h.append("<div class=\"type\">");
                h.append("<span class=\"badge badge-").append(cssClass).append("\">").append(f.getSeverity().name()).append("</span> ");
                h.append(escape(f.getType())).append("</div>");
                h.append("<div class=\"desc\">").append(escape(f.getDescription())).append("</div>");
                h.append("<div class=\"evid\">").append(escape(f.getEvidence())).append("</div>");
                h.append("</div>");
            }
            h.append("</div>");
        }

        // ---- Per-request details ----
        JsonNode requests = session.get("requests");
        if (requests != null && requests.isArray()) {
            int idx = 1;
            for (JsonNode req : requests) {
                appendRequestDetail(h, req, idx++);
            }
        }

        h.append("</div></body></html>");
        return h.toString();
    }

    /**
     * 生成仅静态分析的 HTML 报告（无动态数据）。
     * 见 docs/05-static-analysis.md 第 6 节。
     */
    public static String generateStatic(StaticResult result) {
        List<StaticFinding> findings = result.getFindings();
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"UTF-8\">");
        h.append("<title>CodePerf 静态分析报告</title>");
        h.append("<style>");
        h.append("*{margin:0;padding:0;box-sizing:border-box;}");
        h.append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#333;background:#f5f5f5;}");
        h.append(".header{background:#34495e;color:#fff;padding:24px 32px;}");
        h.append(".header h1{font-size:22px;margin-bottom:8px;}");
        h.append(".header .meta{font-size:13px;opacity:0.85;}");
        h.append(".container{max-width:1100px;margin:0 auto;padding:24px;}");
        h.append(".card{background:#fff;border-radius:6px;box-shadow:0 1px 3px rgba(0,0,0,.1);padding:20px 24px;margin-bottom:20px;}");
        h.append(".card h2{font-size:17px;margin-bottom:14px;padding-bottom:8px;border-bottom:2px solid #eee;}");
        h.append(".badge{display:inline-block;padding:2px 8px;border-radius:3px;font-size:11px;font-weight:700;color:#fff;}");
        h.append(".badge-crit{background:#e74c3c;} .badge-warn{background:#f39c12;} .badge-info{background:#3498db;}");
        h.append(".badge-high{background:#8e44ad;} .badge-medium{background:#16a085;} .badge-low{background:#7f8c8d;}");
        h.append(".finding{margin-bottom:12px;padding:12px 16px;border-left:4px solid #ddd;background:#fafafa;}");
        h.append(".finding.crit{border-left-color:#e74c3c;} .finding.warn{border-left-color:#f39c12;} .finding.info{border-left-color:#3498db;}");
        h.append(".finding .type{font-weight:600;margin-bottom:4px;}");
        h.append(".finding .where{font-size:12px;color:#2980b9;font-family:monospace;margin-bottom:4px;}");
        h.append(".finding .desc{font-size:13px;color:#555;margin-bottom:4px;}");
        h.append(".finding .evid{font-size:12px;color:#888;font-family:monospace;}");
        h.append("</style></head><body>");

        h.append("<div class=\"header\"><h1>CodePerf 静态分析报告</h1>");
        h.append("<div class=\"meta\">目标包: ").append(escape(result.getTargetPackage()));
        h.append(" &nbsp;|&nbsp; 扫描类数: ").append(result.getClassesScanned());
        h.append(" &nbsp;|&nbsp; 发现: ").append(findings.size()).append(" 条</div></div>");

        h.append("<div class=\"container\"><div class=\"card\"><h2>静态发现</h2>");
        if (findings.isEmpty()) {
            h.append("<p style=\"color:#27ae60;\">未发现静态可疑点。</p>");
        } else {
            List<StaticFinding> sorted = findings.stream()
                    .sorted(Comparator
                            .comparingInt((StaticFinding f) -> f.getSeverity().getLevel())
                            .thenComparingInt(f -> f.getConfidence().ordinal())
                            .reversed())
                    .collect(Collectors.toList());
            for (StaticFinding f : sorted) {
                String sevCls = sevClass(f.getSeverity());
                h.append("<div class=\"finding ").append(sevCls).append("\">");
                h.append("<div class=\"type\">");
                h.append("<span class=\"badge badge-").append(sevCls).append("\">")
                        .append(f.getSeverity().name()).append("</span> ");
                h.append("<span class=\"badge badge-").append(f.getConfidence().name().toLowerCase())
                        .append("\">").append(f.getConfidence().name()).append("</span> ");
                h.append(escape(f.getType())).append("</div>");
                h.append("<div class=\"where\">").append(escape(f.getClassMethod())).append("</div>");
                h.append("<div class=\"desc\">").append(escape(f.getDescription())).append("</div>");
                h.append("<div class=\"evid\">").append(escape(f.getEvidence())).append("</div>");
                h.append("</div>");
            }
        }
        h.append("</div></div></body></html>");
        return h.toString();
    }

    private static void summaryItem(StringBuilder h, String cls, int num, String label) {
        h.append("<div class=\"summary-item ").append(cls).append("\">");
        h.append("<div class=\"num\">").append(num).append("</div>");
        h.append("<div class=\"label\">").append(label).append("</div></div>");
    }

    private static void appendRequestDetail(StringBuilder h, JsonNode req, int idx) {
        String httpMethod = req.path("httpMethod").asText("");
        String path = req.path("path").asText("");
        int status = req.path("status").asInt(0);
        long wallMs = req.path("wallTimeMs").asLong(0);
        long allocBytes = req.path("allocBytes").asLong(0);
        double allocMB = allocBytes / (1024.0 * 1024.0);

        h.append("<div class=\"card\"><h2>请求 #").append(idx).append(": ").append(escape(httpMethod)).append(" ").append(escape(path)).append("</h2>");
        h.append("<table>");
        h.append("<tr><td style=\"width:140px;color:#888\">HTTP 状态</td><td>").append(status).append("</td></tr>");
        h.append("<tr><td style=\"color:#888\">耗时</td><td>").append(wallMs).append(" ms</td></tr>");
        h.append("<tr><td style=\"color:#888\">内存分配</td><td>").append(String.format("%.1f MB", allocMB)).append("</td></tr>");
        h.append("<tr><td style=\"color:#888\">线程</td><td>").append(escape(req.path("threadName").asText(""))).append("</td></tr>");
        h.append("</table>");

        // Call tree
        JsonNode callTree = req.get("callTree");
        if (callTree != null) {
            h.append("<h3 style=\"margin:14px 0 8px;font-size:14px;\">调用树</h3>");
            h.append("<div class=\"call-tree\">");
            appendCallTreeNode(h, callTree, 0);
            h.append("</div>");
        }

        // SQL summary
        JsonNode sqls = req.get("sqls");
        if (sqls != null && sqls.isArray() && sqls.size() > 0) {
            h.append("<h3 style=\"margin:14px 0 8px;font-size:14px;\">SQL 汇总</h3>");
            h.append("<table><tr><th>指纹</th><th>样例</th><th>次数</th><th>总耗时</th><th>最大</th><th>慢</th></tr>");
            for (JsonNode sql : sqls) {
                h.append("<tr>");
                h.append("<td>").append(escape(sql.path("fingerprint").asText(""))).append("</td>");
                h.append("<td style=\"font-size:11px;max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;\">").append(escape(sql.path("sampleSql").asText(""))).append("</td>");
                h.append("<td>").append(sql.path("count").asInt(0)).append("</td>");
                h.append("<td>").append(sql.path("totalMs").asLong(0)).append("ms</td>");
                h.append("<td>").append(sql.path("maxMs").asLong(0)).append("ms</td>");
                h.append("<td>").append(sql.path("slow").asBoolean(false) ? "是" : "否").append("</td>");
                h.append("</tr>");
            }
            h.append("</table>");
        }

        // CPU hotspot top N
        JsonNode samples = req.get("samples");
        if (samples != null && samples.isArray() && samples.size() > 0) {
            Map<String, Integer> topCounts = new LinkedHashMap<>();
            for (JsonNode s : samples) {
                JsonNode frames = s.get("frames");
                if (frames != null && frames.isArray() && frames.size() > 0) {
                    String top = frames.get(0).asText("");
                    topCounts.put(top, topCounts.getOrDefault(top, 0) + 1);
                }
            }
            int total = samples.size();
            List<Map.Entry<String, Integer>> sorted = topCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            if (!sorted.isEmpty()) {
                h.append("<h3 style=\"margin:14px 0 8px;font-size:14px;\">CPU 热点 Top 10（栈顶帧）</h3>");
                h.append("<table><tr><th>方法</th><th>采样数</th><th>占比</th></tr>");
                for (Map.Entry<String, Integer> e : sorted) {
                    double pct = 100.0 * e.getValue() / total;
                    h.append("<tr><td style=\"font-size:11px;\">").append(escape(e.getKey())).append("</td>");
                    h.append("<td>").append(e.getValue()).append("/").append(total).append("</td>");
                    h.append("<td>").append(String.format("%.1f%%", pct)).append("</td></tr>");
                }
                h.append("</table>");
            }
        }

        h.append("</div>");
    }

    private static void appendCallTreeNode(StringBuilder h, JsonNode node, int depth) {
        if (node == null) return;
        String method = node.path("method").asText("?");
        int count = node.path("count").asInt(0);
        long totalMs = node.path("totalTimeMs").asLong(0);
        long selfMs = node.path("selfTimeMs").asLong(0);

        // Skip ROOT virtual node display name but still recurse
        if (!"ROOT".equals(method)) {
            for (int i = 0; i < depth; i++) h.append("  ");
            String indent = depth > 0 ? String.join("", Collections.nCopies(depth, "  ")) : "";
            h.append(escape(indent + method))
              .append("  [count=").append(count)
              .append(", total=").append(totalMs).append("ms")
              .append(", self=").append(selfMs).append("ms")
              .append("]\n");
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                appendCallTreeNode(h, child, "ROOT".equals(method) ? depth : depth + 1);
            }
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String sevClass(Severity s) {
        switch (s) {
            case CRITICAL: return "crit";
            case WARN: return "warn";
            default: return "info";
        }
    }
}

