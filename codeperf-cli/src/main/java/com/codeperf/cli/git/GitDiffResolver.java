package com.codeperf.cli.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        if (MODE_STAGED.equalsIgnoreCase(diffMode)) {
            command.add("--cached");
            command.add("--name-only");
            return command;
        }
        command.add("--name-only");
        if (MODE_WORKTREE.equalsIgnoreCase(diffMode)) {
            command.add("HEAD");
            return command;
        }
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
