package com.codeperf.analysis.source;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.source.rule.SourceRule;
import com.codeperf.analysis.source.rule.SourceRuleRegistry;
import com.codeperf.cli.config.StaticScanConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SourceScannerTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_ReportFinding_When_ChangedFileContainsLoopIo() throws Exception {
        Path source = tempDir.resolve("src/main/java/com/acme/OrderService.java");
        Files.createDirectories(source.getParent());
        Files.write(source, (
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));

        SourceScanRequest request = new SourceScanRequest(
                tempDir,
                Arrays.asList(source),
                new StaticScanConfig());

        SourceScanResult result = new SourceScanner().scan(request);

        assertEquals(1, result.getFilesScanned());
        assertEquals(1, result.getFindings().size());
        assertEquals("DB", result.getFindings().get(0).getIoType());
        assertEquals(0, result.getParseErrors().size());
    }

    @Test
    public void should_DeduplicateEquivalentFindings_When_MultipleRulesReportSameRisk() throws Exception {
        Path source = tempDir.resolve("src/main/java/com/acme/OrderService.java");
        Files.createDirectories(source.getParent());
        Files.write(source, "package com.acme; class OrderService {}\n".getBytes(StandardCharsets.UTF_8));
        SourceFinding finding = new SourceFinding(
                "Loop I/O Amplification",
                Severity.WARN,
                SourceFinding.Confidence.HIGH,
                "循环体内存在外部 I/O 调用，生产数据量放大时可能导致接口响应变慢。",
                "数据库访问调用: orderMapper.selectById(id)",
                source.toString().replace('\\', '/'),
                5,
                4,
                6,
                "DB",
                Collections.<CallChainStep>emptyList());
        SourceRule duplicateRule = context -> Arrays.asList(finding);
        SourceScanner scanner = new SourceScanner(
                new JavaAstParser(),
                new SourceRuleRegistry(Arrays.asList(duplicateRule, duplicateRule)));
        SourceScanRequest request = new SourceScanRequest(
                tempDir,
                Arrays.asList(source),
                new StaticScanConfig());

        SourceScanResult result = scanner.scan(request);

        assertEquals(1, result.getFindings().size());
    }
}
