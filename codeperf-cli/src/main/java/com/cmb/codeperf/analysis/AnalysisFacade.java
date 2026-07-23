package com.cmb.codeperf.analysis;

import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分析引擎门面。CLI report 命令委托到此。
 * 见 docs/04-analysis-report.md 第 6 节、docs/05-static-analysis.md 第 4、7 节。
 */
public class AnalysisFacade {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 仅动态分析。
     *
     * @param inputJson  agent 产出的原始数据 JSON 文件路径
     * @param outputHtml HTML 报告输出路径
     * @param failOn     none=0, info=1, warn=2, critical=3
     * @return 退出码：0 通过，非零达到门禁
     */
    public static int analyze(String inputJson, String outputHtml, int failOn) {
        return analyze(inputJson, null, outputHtml, failOn);
    }

    /**
     * 动态分析；若提供 staticJson，则与静态发现做交叉验证后合并。
     */
    public static int analyze(String inputJson, String staticJson, String outputHtml, int failOn) {
        try {
            JsonNode session = MAPPER.readTree(new File(inputJson));

            AnalysisEngine engine = new AnalysisEngine();
            AnalysisEngine.AnalysisResult result = engine.run(session);
            List<Finding> findings = new ArrayList<>(result.getFindings());

            // 交叉验证：把静态发现合并进来。
            if (staticJson != null && !staticJson.trim().isEmpty()) {
                List<StaticFinding> staticFindings = loadStaticFindings(staticJson);
                Set<String> dynamicMethods = new HashSet<>();
                collectDynamicMethods(session, dynamicMethods);
                findings.addAll(crossValidate(staticFindings, dynamicMethods));
            }

            String html = HtmlReport.generate(session, findings);
            Files.write(Paths.get(outputHtml), html.getBytes(StandardCharsets.UTF_8));
            System.out.println("[codeperf] 报告已生成: " + outputHtml);

            Severity maxSev = maxSeverity(findings);
            if (maxSev == null) {
                System.out.println("[codeperf] 未发现性能问题。");
                return 0;
            }

            System.out.println("[codeperf] 发现 " + findings.size() + " 个问题，最高严重度: " + maxSev.name());

            if (maxSev.getLevel() >= failOn) {
                System.out.println("[codeperf] 门禁不通过 (阈值 " + failOn + ", 实际最高 " + maxSev.getLevel() + ")");
                return maxSev.getLevel();
            }
            return 0;

        } catch (IOException e) {
            System.err.println("[codeperf] 分析失败: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return -1;
        }
    }

    // ---------------- 交叉验证 ----------------

    /**
     * 交叉验证矩阵（见 docs/05-static-analysis.md 第 4 节）的 MVP 实现：
     * <ul>
     *   <li>静态发现的方法在动态调用树中被执行 → 已确认，confidence 升至 HIGH，保留 severity。</li>
     *   <li>静态发现的方法未被动态执行（未采集/未触发）→ 待验证，confidence 降一级，保留 severity。</li>
     * </ul>
     */
    private static List<Finding> crossValidate(List<StaticFinding> staticFindings, Set<String> dynamicMethods) {
        List<Finding> merged = new ArrayList<>();
        for (StaticFinding sf : staticFindings) {
            boolean exercised = isExercised(sf.getClassMethod(), dynamicMethods);
            String status;
            StaticFinding.Confidence conf;
            if (exercised) {
                status = "已确认";
                conf = StaticFinding.Confidence.HIGH;
            } else {
                status = "待验证·需动态验证";
                conf = downgrade(sf.getConfidence());
            }
            String type = "[" + status + "] " + sf.getType();
            String desc = sf.getDescription()
                    + "（来源方法 " + sf.getClassMethod()
                    + "，静态置信度 " + conf.name() + "）";
            merged.add(new Finding(type, sf.getSeverity(), desc, sf.getEvidence()));
        }
        return merged;
    }

    private static StaticFinding.Confidence downgrade(StaticFinding.Confidence c) {
        switch (c) {
            case HIGH:   return StaticFinding.Confidence.MEDIUM;
            case MEDIUM: return StaticFinding.Confidence.LOW;
            default:     return StaticFinding.Confidence.LOW;
        }
    }

    /** 动态方法名后缀匹配静态来源方法（忽略参数签名）。 */
    private static boolean isExercised(String staticClassMethod, Set<String> dynamicMethods) {
        if (staticClassMethod == null || staticClassMethod.isEmpty()) return false;
        for (String dyn : dynamicMethods) {
            String norm = normalize(dyn);
            if (norm.endsWith(staticClassMethod) || norm.contains(staticClassMethod)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String method) {
        if (method == null) return "";
        String m = method.replace('#', '.').replace('/', '.');
        int paren = m.indexOf('(');
        if (paren >= 0) m = m.substring(0, paren);
        return m;
    }

    /** 递归收集动态调用树中出现的所有方法名。 */
    private static void collectDynamicMethods(JsonNode session, Set<String> out) {
        JsonNode requests = session.get("requests");
        if (requests == null || !requests.isArray()) return;
        for (JsonNode req : requests) {
            collectFromNode(req.get("callTree"), out);
        }
    }

    private static void collectFromNode(JsonNode node, Set<String> out) {
        if (node == null) return;
        String method = node.path("method").asText("");
        if (!method.isEmpty() && !"ROOT".equals(method)) {
            out.add(method);
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectFromNode(child, out);
            }
        }
    }

    private static List<StaticFinding> loadStaticFindings(String staticJson) throws IOException {
        JsonNode root = MAPPER.readTree(new File(staticJson));
        JsonNode arr = root.get("findings");
        List<StaticFinding> list = new ArrayList<>();
        if (arr == null || !arr.isArray()) return list;
        for (JsonNode f : arr) {
            String type = f.path("type").asText("");
            Severity severity = parseSeverity(f.path("severity").asText("WARN"));
            StaticFinding.Confidence confidence = parseConfidence(f.path("confidence").asText("LOW"));
            String description = f.path("description").asText("");
            String evidence = f.path("evidence").asText("");
            String classMethod = f.path("classMethod").asText("");
            list.add(new StaticFinding(type, severity, confidence, description, evidence, classMethod));
        }
        return list;
    }

    private static Severity parseSeverity(String s) {
        try {
            return Severity.valueOf(s);
        } catch (Exception e) {
            return Severity.WARN;
        }
    }

    private static StaticFinding.Confidence parseConfidence(String s) {
        try {
            return StaticFinding.Confidence.valueOf(s);
        } catch (Exception e) {
            return StaticFinding.Confidence.LOW;
        }
    }

    private static Severity maxSeverity(List<Finding> findings) {
        if (findings.isEmpty()) return null;
        Severity max = Severity.INFO;
        for (Finding f : findings) {
            if (f.getSeverity().getLevel() > max.getLevel()) {
                max = f.getSeverity();
            }
        }
        return max;
    }
}

