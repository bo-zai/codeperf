package com.cmb.codeperf.cli.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitLocalChangeResolverTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_CollectCommittedStagedUnstagedAndUntrackedJavaFiles_When_LocalPreScanRuns() throws Exception {
        initGitRepoWithUpstream();
        write("src/main/java/com/acme/CommittedService.java", "class CommittedService {}\n");
        runGit("add", "src/main/java/com/acme/CommittedService.java");
        runGit("commit", "-m", "add committed service");

        write("src/main/java/com/acme/StagedService.java", "class StagedService {}\n");
        runGit("add", "src/main/java/com/acme/StagedService.java");

        write("src/main/java/com/acme/TrackedService.java", "class TrackedService {}\n");
        runGit("add", "src/main/java/com/acme/TrackedService.java");
        runGit("commit", "-m", "add tracked service");
        write("src/main/java/com/acme/TrackedService.java", "class TrackedService { void changed() {} }\n");

        write("src/main/java/com/acme/UntrackedService.java", "class UntrackedService {}\n");
        write("README.md", "# ignored\n");

        GitLocalChangeSet changeSet = new GitLocalChangeResolver().resolve(tempDir);

        assertTrue(changeSet.getCommittedNotPushedCount() >= 1);
        assertTrue(changeSet.getStagedCount() >= 0);
        assertTrue(changeSet.getUnstagedCount() >= 0);
        assertEquals(1, changeSet.getUntrackedCount());
        assertTrue(changeSet.getSourceFiles().contains(tempDir.resolve("src/main/java/com/acme/CommittedService.java").toAbsolutePath().normalize()));
        assertTrue(changeSet.getSourceFiles().contains(tempDir.resolve("src/main/java/com/acme/StagedService.java").toAbsolutePath().normalize()));
        assertTrue(changeSet.getSourceFiles().contains(tempDir.resolve("src/main/java/com/acme/TrackedService.java").toAbsolutePath().normalize()));
        assertTrue(changeSet.getSourceFiles().contains(tempDir.resolve("src/main/java/com/acme/UntrackedService.java").toAbsolutePath().normalize()));
        assertEquals(false, changeSet.getSourceFiles().contains(tempDir.resolve("README.md").toAbsolutePath().normalize()));
    }

    private void initGitRepoWithUpstream() throws Exception {
        Path remote = Files.createTempDirectory(tempDir.getParent(), "remote");
        runGit(remote, "init", "--bare");
        runGit("init");
        runGit("config", "user.email", "codeperf@example.com");
        runGit("config", "user.name", "CodePerf Test");
        write("README.md", "# demo\n");
        runGit("add", ".");
        runGit("commit", "-m", "init");
        runGit("branch", "-M", "feature");
        runGit("remote", "add", "origin", remote.toString());
        runGit("push", "-u", "origin", "feature");
    }

    private void write(String file, String content) throws Exception {
        Path path = tempDir.resolve(file);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void runGit(String... args) throws Exception {
        runGit(tempDir, args);
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
}
