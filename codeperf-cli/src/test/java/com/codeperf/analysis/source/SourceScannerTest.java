package com.codeperf.analysis.source;

import com.codeperf.cli.config.StaticScanConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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
}
