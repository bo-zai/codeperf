package com.cmb.codeperf.cli.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitDiffResolverTest {

    @TempDir
    private Path tempDir;

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

    @Test
    public void should_ResolveChangedJavaFiles_When_GitDiffExecuted() throws Exception {
        runGit("init");
        runGit("config", "user.email", "codeperf@example.com");
        runGit("config", "user.name", "CodePerf Test");
        write("src/main/java/com/acme/OrderService.java", "class OrderService {}\n");
        write("README.md", "# demo\n");
        runGit("add", ".");
        runGit("commit", "-m", "init");

        write("src/main/java/com/acme/OrderService.java", "class OrderService { void changed() {} }\n");
        write("README.md", "# changed\n");

        List<String> files = GitDiffResolver.changedJavaFiles(tempDir, "HEAD", "--");

        assertEquals(Arrays.asList("src/main/java/com/acme/OrderService.java"), files);
    }

    @Test
    public void should_ResolveStagedJavaFiles_When_PreCommitModeUsed() throws Exception {
        runGit("init");
        runGit("config", "user.email", "codeperf@example.com");
        runGit("config", "user.name", "CodePerf Test");
        write("src/main/java/com/acme/OrderService.java", "class OrderService {}\n");
        runGit("add", ".");
        runGit("commit", "-m", "init");

        write("src/main/java/com/acme/OrderService.java", "class OrderService { void staged() {} }\n");
        write("src/main/java/com/acme/UserService.java", "class UserService {}\n");
        runGit("add", "src/main/java/com/acme/OrderService.java");

        List<String> files = GitDiffResolver.changedJavaFiles(
                tempDir, "release/base", "HEAD", GitDiffResolver.MODE_STAGED);

        assertEquals(Arrays.asList("src/main/java/com/acme/OrderService.java"), files);
    }

    @Test
    public void should_ResolvePushRange_When_PrePushEnvironmentProvided() {
        GitPushRange range = GitPushRange.fromEnvironment(new GitPushRange.EnvironmentReader() {
            @Override
            public String get(String name) {
                if ("CODEPERF_PUSH_OLD_SHA".equals(name)) {
                    return "1111111";
                }
                if ("CODEPERF_PUSH_NEW_SHA".equals(name)) {
                    return "2222222";
                }
                if ("CODEPERF_PUSH_REMOTE_BRANCH".equals(name)) {
                    return "develop";
                }
                return "";
            }
        });

        assertEquals("1111111", range.getBaseRef());
        assertEquals("2222222", range.getHeadRef());
        assertEquals("develop", range.getRemoteBranch());
        assertEquals(true, range.isPresent());
    }

    @Test
    public void should_IgnoreDeletedRemoteBranch_When_PrePushNewShaIsZero() {
        GitPushRange range = GitPushRange.fromEnvironment(new GitPushRange.EnvironmentReader() {
            @Override
            public String get(String name) {
                if ("CODEPERF_PUSH_OLD_SHA".equals(name)) {
                    return "1111111";
                }
                if ("CODEPERF_PUSH_NEW_SHA".equals(name)) {
                    return "0000000000000000000000000000000000000000";
                }
                return "";
            }
        });

        assertEquals(false, range.isPresent());
    }

    private void write(String file, String content) throws Exception {
        Path path = tempDir.resolve(file);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void runGit(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("git command failed: " + Arrays.toString(command));
        }
    }
}

