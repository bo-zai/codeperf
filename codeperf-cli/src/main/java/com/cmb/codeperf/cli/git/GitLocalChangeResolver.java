package com.cmb.codeperf.cli.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 本地预检变更解析器。
 * <p>
 * 手动预检关注开发者本地正在写的代码，因此需要同时覆盖：
 * 已提交未推送、已暂存、未暂存和未跟踪 Java 文件。
 */
public class GitLocalChangeResolver {

    public GitLocalChangeSet resolve(Path workingDirectory) throws IOException {
        List<String> committed = committedNotPushedFiles(workingDirectory);
        List<String> staged = runGit(workingDirectory, "diff", "--cached", "--name-only");
        List<String> unstaged = runGit(workingDirectory, "diff", "--name-only");
        List<String> untracked = runGit(workingDirectory, "ls-files", "--others", "--exclude-standard");

        Set<Path> files = new LinkedHashSet<Path>();
        addJavaFiles(workingDirectory, files, committed);
        addJavaFiles(workingDirectory, files, staged);
        addJavaFiles(workingDirectory, files, unstaged);
        addJavaFiles(workingDirectory, files, untracked);

        return new GitLocalChangeSet(
                new ArrayList<Path>(files),
                countJavaFiles(committed),
                countJavaFiles(staged),
                countJavaFiles(unstaged),
                countJavaFiles(untracked));
    }

    private List<String> committedNotPushedFiles(Path workingDirectory) {
        try {
            String upstream = resolveUpstreamRef(workingDirectory);
            if (upstream.isEmpty()) {
                return new ArrayList<String>();
            }
            String mergeBase = singleLine(runGit(workingDirectory, "merge-base", "HEAD", upstream));
            if (mergeBase.isEmpty()) {
                return new ArrayList<String>();
            }
            return runGit(workingDirectory, "diff", "--name-only", mergeBase, "HEAD");
        } catch (IOException e) {
            return new ArrayList<String>();
        }
    }

    private String resolveUpstreamRef(Path workingDirectory) throws IOException {
        String branch = singleLine(runGit(workingDirectory, "branch", "--show-current"));
        if (branch.isEmpty()) {
            return "";
        }
        String remote = singleLine(runGit(workingDirectory, "config", "--get", "branch." + branch + ".remote"));
        String merge = singleLine(runGit(workingDirectory, "config", "--get", "branch." + branch + ".merge"));
        if (remote.isEmpty() || merge.isEmpty()) {
            return "";
        }
        if (merge.startsWith("refs/heads/")) {
            merge = merge.substring("refs/heads/".length());
        }
        return remote + "/" + merge;
    }

    private void addJavaFiles(Path workingDirectory, Set<Path> files, List<String> values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.endsWith(".java")) {
                continue;
            }
            Path path = workingDirectory.resolve(normalized).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                files.add(path);
            }
        }
    }

    private int countJavaFiles(List<String> values) {
        int count = 0;
        for (String value : values) {
            if (normalize(value).endsWith(".java")) {
                count++;
            }
        }
        return count;
    }

    private List<String> runGit(Path workingDirectory, String... args) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add("git");
        for (String arg : args) {
            command.add(arg);
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> output = readLines(process);
        int exitCode = waitFor(process);
        if (exitCode != 0) {
            throw new IOException("git 命令执行失败，exitCode=" + exitCode + ", output=" + output);
        }
        return output;
    }

    private List<String> readLines(Process process) throws IOException {
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 git 命令执行被中断", e);
        }
    }

    private String singleLine(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(0).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('\\', '/');
    }
}
