package com.cmb.codeperf.cli.git;

import com.cmb.codeperf.cli.config.StaticScanConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitScanRangeResolverTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_ResolvePrePushRange_When_EnvironmentProvided() {
        GitScanRange range = new GitScanRangeResolver().resolve(tempDir, new StaticScanConfig(), new GitPushRange.EnvironmentReader() {
            @Override
            public String get(String name) {
                if ("CODEPERF_PUSH_OLD_SHA".equals(name)) {
                    return "1111111";
                }
                if ("CODEPERF_PUSH_NEW_SHA".equals(name)) {
                    return "2222222";
                }
                if ("CODEPERF_PUSH_REMOTE_BRANCH".equals(name)) {
                    return "feature/demo";
                }
                return "";
            }
        });

        assertEquals("pre-push", range.getSource());
        assertEquals("1111111", range.getBaseRef());
        assertEquals("2222222", range.getHeadRef());
        assertEquals(GitDiffResolver.MODE_RANGE, range.getDiffMode());
        assertEquals("feature/demo", range.getRemoteBranch());
    }

    @Test
    public void should_ResolveUpstreamRange_When_UpstreamConfigured() throws Exception {
        initGitRepoWithUpstream();
        StaticScanConfig config = new StaticScanConfig();
        config.setBaseRef("release/base");
        config.setHeadRef("HEAD");

        GitScanRange range = new GitScanRangeResolver().resolve(tempDir, config, emptyEnv());
        String expectedMergeBase = runGitOutput(tempDir, "merge-base", "HEAD", "origin/feature");

        assertEquals("upstream", range.getSource());
        assertEquals("HEAD", range.getHeadRef());
        assertEquals(expectedMergeBase, range.getBaseRef());
        assertEquals("origin/feature", range.getUpstreamRef());
        assertEquals(GitDiffResolver.MODE_RANGE, range.getDiffMode());
    }

    @Test
    public void should_ResolveConfigRange_When_NoUpstreamAndConfigProvided() throws Exception {
        initGitRepoWithoutUpstream();
        StaticScanConfig config = new StaticScanConfig();
        config.setBaseRef("release/base");
        config.setHeadRef("HEAD");

        GitScanRange range = new GitScanRangeResolver().resolve(tempDir, config, emptyEnv());

        assertEquals("config", range.getSource());
        assertEquals("release/base", range.getBaseRef());
        assertEquals("HEAD", range.getHeadRef());
        assertEquals(GitDiffResolver.MODE_RANGE, range.getDiffMode());
    }

    @Test
    public void should_ResolveWorktreeRange_When_NoOtherBaselineAvailable() {
        StaticScanConfig config = new StaticScanConfig();
        config.setBaseRef("");
        config.setHeadRef("");

        GitScanRange range = new GitScanRangeResolver().resolve(tempDir, config, emptyEnv());

        assertEquals("worktree", range.getSource());
        assertEquals("", range.getBaseRef());
        assertEquals("", range.getHeadRef());
        assertEquals(GitDiffResolver.MODE_WORKTREE, range.getDiffMode());
    }

    private GitPushRange.EnvironmentReader emptyEnv() {
        return new GitPushRange.EnvironmentReader() {
            @Override
            public String get(String name) {
                return "";
            }
        };
    }

    private void initGitRepoWithUpstream() throws Exception {
        Path remote = Files.createTempDirectory(tempDir.getParent(), "remote");
        runGit(remote, "init", "--bare");

        runGit(tempDir, "init");
        runGit(tempDir, "config", "user.email", "codeperf@example.com");
        runGit(tempDir, "config", "user.name", "CodePerf Test");
        write(tempDir, "README.md", "# demo\n");
        runGit(tempDir, "add", ".");
        runGit(tempDir, "commit", "-m", "init");
        runGit(tempDir, "branch", "-M", "feature");
        runGit(tempDir, "remote", "add", "origin", remote.toString());
        runGit(tempDir, "push", "-u", "origin", "feature");
    }

    private void initGitRepoWithoutUpstream() throws Exception {
        runGit(tempDir, "init");
        runGit(tempDir, "config", "user.email", "codeperf@example.com");
        runGit(tempDir, "config", "user.name", "CodePerf Test");
        write(tempDir, "README.md", "# demo\n");
        runGit(tempDir, "add", ".");
        runGit(tempDir, "commit", "-m", "init");
    }

    private void write(Path root, String file, String content) throws Exception {
        Path path = root.resolve(file);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void runGit(Path directory, String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("git command failed: " + Arrays.toString(command));
        }
    }

    private String runGitOutput(Path directory, String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        byte[] buffer = readStream(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("git command failed: " + Arrays.toString(command));
        }
        return new String(buffer, StandardCharsets.UTF_8).trim();
    }

    private byte[] readStream(java.io.InputStream input) throws Exception {
        byte[] buffer = new byte[1024];
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
