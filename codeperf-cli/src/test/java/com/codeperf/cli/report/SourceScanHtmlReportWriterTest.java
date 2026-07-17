package com.codeperf.cli.report;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.source.CallChainStep;
import com.codeperf.analysis.source.RiskAttribution;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.SourceScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SourceScanHtmlReportWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_WriteDeveloperFocusedHtmlReport_When_SourceScanResultProvided() throws Exception {
        writeSource("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id); // <risk>\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        RiskAttribution attribution = new RiskAttribution(
                RiskAttribution.RiskScope.UNKNOWN,
                false,
                RiskAttribution.AttributionConfidence.LOW,
                "",
                "",
                "",
                "",
                "");
        SourceFinding finding = new SourceFinding(
                "LOOP_IO_AMPLIFICATION",
                Severity.WARN,
                SourceFinding.Confidence.HIGH,
                "循环中存在外部 I/O",
                "orderMapper.selectById(id) <risk>",
                "src/main/java/com/acme/OrderService.java",
                6,
                5,
                7,
                "DB",
                Collections.singletonList(new CallChainStep(
                        "OrderService",
                        "buildReport",
                        "src/main/java/com/acme/OrderService.java",
                        6)),
                "buildReport",
                6,
                6,
                attribution);
        SourceScanResult result = new SourceScanResult(
                1,
                Collections.singletonList(finding),
                Arrays.asList("Broken.java: unexpected <token>"));

        Path output = tempDir.resolve("source-report.html");
        new SourceScanHtmlReportWriter().write(output, result, tempDir);

        String html = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        assertTrue(html.contains("CodePerf 本地代码扫描报告"));
        assertTrue(html.contains("质量门禁"));
        assertTrue(html.contains("检测失败"));
        assertTrue(html.contains("class=\"filter-toolbar\""));
        assertTrue(html.contains("class=\"issue-feed\""));
        assertTrue(html.contains("class=\"floating-toc\""));
        assertTrue(html.contains("class=\"detail-card active\""));
        assertTrue(html.contains("class=\"issue-row active\""));
        assertTrue(html.contains("data-detail"));
        assertTrue(html.contains("data-module=\"root\""));
        assertTrue(html.contains("data-io=\"DB\""));
        assertTrue(html.contains("data-confidence=\"HIGH\""));
        assertTrue(html.contains("data-scope=\"UNKNOWN\""));
        assertTrue(html.contains("循环内外部 I/O 调用可能被生产数据量放大"));
        assertTrue(html.contains("src/main/java/com/acme/OrderService.java"));
        assertTrue(html.contains("OrderService.java:6"));
        assertTrue(html.contains("源码片段"));
        assertTrue(html.contains("调用链"));
        assertTrue(html.contains("修复建议"));
        assertTrue(html.contains("未归因"));
        assertTrue(html.contains("orderMapper.selectById(id); // &lt;risk&gt;"));
        assertTrue(html.contains("orderMapper.selectById(id) &lt;risk&gt;"));
        assertTrue(html.contains("Broken.java: unexpected &lt;token&gt;"));
        assertFalse(html.contains("<table"));
        assertFalse(html.contains("top-card"));
        assertFalse(html.contains("hero"));
    }

    @Test
    public void should_WriteFileIssueSummaryAndNestedNavigation_When_MultipleFindingsInSameFile() throws Exception {
        writeSource("src/main/java/com/acme/PrePushRiskDemoService.java",
                "package com.acme;\n"
                        + "class PrePushRiskDemoService {\n"
                        + "  void buildOrderViews(java.util.List<Long> orderIds) {\n"
                        + "    for (Long orderId : orderIds) {\n"
                        + "      orderMapper.selectById(orderId);\n"
                        + "    }\n"
                        + "  }\n"
                        + "  void buildDeliveryViews(java.util.List<Long> orderIds) {\n"
                        + "    for (Long orderId : orderIds) {\n"
                        + "      orderMapper.selectById(orderId);\n"
                        + "      deliveryClient.queryDeliveryStatus(orderId);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        SourceFinding first = finding("PrePushRiskDemoService", "buildOrderViews", 5, "DB",
                SourceFinding.Confidence.HIGH, "orderMapper.selectById(orderId)");
        SourceFinding second = finding("PrePushRiskDemoService", "buildDeliveryViews", 10, "DB",
                SourceFinding.Confidence.HIGH, "orderMapper.selectById(orderId)");
        SourceFinding third = finding("PrePushRiskDemoService", "buildDeliveryViews", 11, "SDK",
                SourceFinding.Confidence.MEDIUM, "deliveryClient.queryDeliveryStatus(orderId)");
        SourceScanResult result = new SourceScanResult(1, Arrays.asList(first, second, third), Collections.emptyList());

        Path output = tempDir.resolve("summary-report.html");
        new SourceScanHtmlReportWriter().write(output, result, tempDir);

        String html = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        assertTrue(html.contains("class=\"file-card\""));
        assertTrue(html.contains("PrePushRiskDemoService.java"));
        assertTrue(html.contains("3 个问题"));
        assertTrue(html.contains("HIGH 2"));
        assertTrue(html.contains("MEDIUM 1"));
        assertTrue(html.contains("DB 2"));
        assertTrue(html.contains("SDK 1"));
        assertTrue(html.contains("id=\"issue-0\""));
        assertTrue(html.contains("id=\"issue-1\""));
        assertTrue(html.contains("id=\"issue-2\""));
        assertTrue(html.contains("id=\"detail-0\""));
        assertTrue(html.contains("id=\"detail-1\""));
        assertTrue(html.contains("id=\"detail-2\""));
        assertTrue(html.contains(">L5<"));
        assertTrue(html.contains(">L10<"));
        assertTrue(html.contains(">L11<"));
        assertTrue(html.contains("data-target=\"detail-0\""));
        assertTrue(html.contains("data-target=\"detail-1\""));
        assertTrue(html.contains("data-target=\"detail-2\""));
    }

    @Test
    public void should_ShowSourceUnavailable_When_SourceFileMissing() throws Exception {
        SourceFinding finding = new SourceFinding(
                "LOOP_IO_AMPLIFICATION",
                Severity.WARN,
                SourceFinding.Confidence.MEDIUM,
                "循环中存在外部 I/O",
                "remoteClient.get(id)",
                "src/main/java/com/acme/MissingService.java",
                10,
                8,
                12,
                "SDK",
                Collections.emptyList(),
                "load",
                9,
                10,
                RiskAttribution.unknown());
        SourceScanResult result = new SourceScanResult(1, Collections.singletonList(finding), Collections.emptyList());

        Path output = tempDir.resolve("missing-source-report.html");
        new SourceScanHtmlReportWriter().write(output, result, tempDir);

        String html = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        assertTrue(html.contains("源码片段不可用"));
        assertTrue(html.contains("本次扫描未获得增量归因"));
    }

    private void writeSource(String file, String content) throws Exception {
        Path path = tempDir.resolve(file);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private SourceFinding finding(String className, String methodName, int lineNumber, String ioType,
            SourceFinding.Confidence confidence, String evidence) {
        return new SourceFinding(
                "LOOP_IO_AMPLIFICATION",
                Severity.WARN,
                confidence,
                "循环中存在外部 I/O",
                evidence,
                "src/main/java/com/acme/" + className + ".java",
                lineNumber,
                Math.max(1, lineNumber - 1),
                lineNumber + 1,
                ioType,
                Collections.emptyList(),
                methodName,
                lineNumber,
                lineNumber,
                RiskAttribution.unknown());
    }
}
