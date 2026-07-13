package com.codeperf.cli.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

/**
 * 解析 Git 元信息。
 * CI 平台优先使用环境变量，本地或普通 Git hook 回退到 git rev-parse。
 */
public final class GitMetadataResolver {

    private GitMetadataResolver() {
    }

    public static String resolveCommit(Path workingDirectory, Map<String, String> env) throws IOException {
        String value = firstNonBlank(env, "CI_COMMIT_SHA", "GITHUB_SHA", "GIT_COMMIT");
        return value == null ? runGit(workingDirectory, "rev-parse", "HEAD") : value;
    }

    public static String resolveBranch(Path workingDirectory, Map<String, String> env) throws IOException {
        String value = firstNonBlank(env, "CI_COMMIT_BRANCH", "GITHUB_REF_NAME", "BRANCH_NAME");
        return value == null ? runGit(workingDirectory, "rev-parse", "--abbrev-ref", "HEAD") : value;
    }

    static String firstNonBlank(Map<String, String> env, String... names) {
        if (env == null || names == null) {
            return null;
        }
        for (String name : names) {
            String value = env.get(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String runGit(Path workingDirectory, String... args) throws IOException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = readOutput(process);
        int exitCode = waitFor(process);
        if (exitCode != 0) {
            throw new IOException("git 元信息解析失败，exitCode=" + exitCode + ", output=" + output);
        }
        return output.trim();
    }

    private static String readOutput(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 git 元信息进程被中断", e);
        }
    }
}
