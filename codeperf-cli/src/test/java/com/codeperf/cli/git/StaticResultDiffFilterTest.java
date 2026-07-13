package com.codeperf.cli.git;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.staticanalysis.StaticFinding;
import com.codeperf.analysis.staticanalysis.StaticResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StaticResultDiffFilterTest {

    @Test
    public void should_KeepOnlyChangedSourceFindings_When_FilterByGitDiff() {
        StaticFinding changed = finding("codeperf-demo/src/main/java/com/acme/OrderService.java");
        StaticFinding unchanged = finding("codeperf-demo/src/main/java/com/acme/UserService.java");
        StaticResult result = new StaticResult("com.acme", 2, Arrays.asList(changed, unchanged));

        StaticResult filtered = StaticResultDiffFilter.filter(result, Arrays.asList(
                "codeperf-demo/src/main/java/com/acme/OrderService.java"));

        assertEquals(1, filtered.getFindings().size());
        assertEquals("codeperf-demo/src/main/java/com/acme/OrderService.java",
                filtered.getFindings().get(0).getSourceFile());
    }

    @Test
    public void should_ReturnEmptyFindings_When_NoJavaFileChanged() {
        StaticFinding finding = finding("codeperf-demo/src/main/java/com/acme/OrderService.java");
        StaticResult result = new StaticResult("com.acme", 1, Arrays.asList(finding));

        StaticResult filtered = StaticResultDiffFilter.filter(result, Arrays.asList());

        assertEquals(0, filtered.getFindings().size());
    }

    private StaticFinding finding(String sourceFile) {
        return new StaticFinding(
                "Loop I/O Amplification",
                Severity.WARN,
                StaticFinding.Confidence.HIGH,
                "desc",
                "evidence",
                "com.acme.Service.method",
                sourceFile,
                10,
                8,
                12,
                "com.acme.Service",
                "method",
                "com.acme.Client",
                "call",
                "HTTP");
    }
}
