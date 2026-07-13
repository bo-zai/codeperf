package com.codeperf.cli.git;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitMetadataResolverTest {

    @Test
    public void should_UseFirstNonBlankValue_When_CiEnvProvided() {
        Map<String, String> env = new HashMap<>();
        env.put("CI_COMMIT_SHA", " ");
        env.put("GITHUB_SHA", "abc123");
        env.put("GIT_COMMIT", "def456");

        String value = GitMetadataResolver.firstNonBlank(env,
                "CI_COMMIT_SHA", "GITHUB_SHA", "GIT_COMMIT");

        assertEquals("abc123", value);
    }
}
