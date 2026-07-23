package com.cmb.codeperf.cli.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectContextResolverTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_ResolveGitRootAndConfig_When_CommandRunsFromSubdirectory() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"),
                "project: demo\n".getBytes(StandardCharsets.UTF_8));
        Path subdir = tempDir.resolve("module/src/main/java");
        Files.createDirectories(subdir);

        ProjectContext context = new ProjectContextResolver().resolve(subdir);

        assertEquals(tempDir.toAbsolutePath().normalize(), context.getRootDirectory());
        assertEquals(tempDir.resolve(".codeperf.yml").toAbsolutePath().normalize(), context.getConfigPath());
        assertEquals("demo", context.getConfig().getProject());
    }

    @Test
    public void should_ResolveConfiguredPathAgainstGitRoot_When_PathProvided() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.write(tempDir.resolve(".codeperf.yml"),
                "project: demo\n".getBytes(StandardCharsets.UTF_8));

        ProjectContext context = new ProjectContextResolver().resolve(tempDir);

        assertEquals(tempDir.resolve("src/main/java").toAbsolutePath().normalize(),
                context.resolvePath("src/main/java"));
    }
}

