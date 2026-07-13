package com.codeperf.cli.report;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.SourceScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SourceScanJsonReportWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_WriteJsonReport_When_SourceScanResultProvided() throws Exception {
        SourceFinding finding = new SourceFinding(
                "Loop I/O Amplification",
                Severity.WARN,
                SourceFinding.Confidence.HIGH,
                "desc",
                "evidence",
                "src/main/java/OrderService.java",
                5,
                4,
                6,
                "DB",
                Collections.emptyList());
        SourceScanResult result = new SourceScanResult(1, Arrays.asList(finding), Collections.emptyList());
        Path output = tempDir.resolve("codeperf-source-report.json");

        new SourceScanJsonReportWriter().write(output, result);

        String json = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        assertTrue(json.contains("Loop I/O Amplification"));
        assertTrue(json.contains("src/main/java/OrderService.java"));
    }
}
