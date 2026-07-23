package com.cmb.codeperf.cli.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Git diff 解析器：获取变更的 Java 文件路径列表。
 * <p>
 * 支持三种 diff 模式：
 * <ul>
 *   <li>range（默认）：比较 baseRef 到 headRef 之间的变更，用于 CI 和分支对比</li>
 *   <li>staged：获取已暂存但未提交的变更，用于 pre-commit 钩子</li>
 *   <li>worktree：获取工作区所有未提交变更（含暂存），用于本地检查</li>
 * </ul>
 * <p>
 * 设计决策：
 * <ul>
 *   <li>仅返回 .java 文件：静态分析只处理 Java 源码</li>
 *   <li>路径规范化：返回绝对路径且统一使用正斜杠，便于跨平台处理</li>
 * </ul>
 */
public final class GitDiffResolver {

    public static final String MODE_RANGE = "range";
    public static final String MODE_STAGED = "staged";
    public static final String MODE_WORKTREE = "worktree";

    private GitDiffResolver() {
    }

    public static List<String> changedJavaFiles(Path workingDirectory, String base, String head) throws IOException {
        return changedJavaFiles(workingDirectory, base, head, MODE_RANGE);
    }

    public static List<String> changedJavaFiles(Path workingDirectory, String base, String head,
                                                String diffMode) throws IOException {
        List<String> output = runGitNameOnlyDiff(workingDirectory, base, head, diffMode);
        return parseChangedJavaFiles(output);
    }

    public static List<Path> changedJavaFilePaths(Path workingDirectory, String base, String head,
                                                  String diffMode) throws IOException {
        List<String> changed = changedJavaFiles(workingDirectory, base, head, diffMode);
        List<Path> paths = new ArrayList<>();
        for (String file : changed) {
            Path path = workingDirectory.resolve(file).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    public static List<String> parseChangedJavaFiles(List<String> nameOnlyOutput) {
        List<String> files = new ArrayList<>();
        if (nameOnlyOutput == null) {
            return files;
        }
        for (String line : nameOnlyOutput) {
            if (line == null) {
                continue;
            }
            String path = line.trim().replace('\\', '/');
            if (path.endsWith(".java")) {
                files.add(path);
            }
        }
        return files;
    }

    private static List<String> runGitNameOnlyDiff(Path workingDirectory, String base, String head,
                                                   String diffMode) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(buildCommand(base, head, diffMode));
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> output = readLines(process);
        int exitCode = waitFor(process);
        if (exitCode != 0) {
            throw new IOException("git diff 执行失败，exitCode=" + exitCode + ", output=" + output);
        }
        return output;
    }

    private static List<String> buildCommand(String base, String head, String diffMode) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("diff");
        // staged 模式：比较暂存区和 HEAD，用于 pre-commit 钩子
        if (GitDiffResolver.MODE_STAGED.equalsIgnoreCase(diffMode)) {
            command.add("--cached");
            command.add("--name-only");
            return command;
        }
        command.add("--name-only");
        // worktree 模式：比较工作区和 HEAD，用于本地检查
        if (GitDiffResolver.MODE_WORKTREE.equalsIgnoreCase(diffMode)) {
            command.add("HEAD");
            return command;
        }
        // range 模式（默认）：比较 base 和 head 两个引用，用于 CI 和分支对比
        command.add(base);
        command.add(head);
        return command;
    }

    private static List<String> readLines(Process process) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 git diff 进程被中断", e);
        }
    }
}

