package com.codeperf.cli.git;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitDiffResolverTest {

    @Test
    public void should_ParseOnlyJavaFiles_When_GitNameOnlyOutputProvided() {
        List<String> files = GitDiffResolver.parseChangedJavaFiles(Arrays.asList(
                "src/main/java/com/acme/OrderService.java",
                "README.md",
                "src/test/java/com/acme/OrderServiceTest.java",
                "pom.xml"));

        assertEquals(Arrays.asList(
                "src/main/java/com/acme/OrderService.java",
                "src/test/java/com/acme/OrderServiceTest.java"), files);
    }
}
