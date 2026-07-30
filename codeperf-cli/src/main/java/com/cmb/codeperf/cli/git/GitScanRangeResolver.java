package com.cmb.codeperf.cli.git;

import com.cmb.codeperf.cli.config.StaticScanConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 扫描基线解析器：按照运行场景选择本次 diff 范围。
 * <p>
 * 优先级：
 * <ol>
 *   <li>pre-push hook 传入的 old/new SHA</li>
 *   <li>当前分支 upstream 的 merge-base</li>
 *   <li>配置中的 baseRef/headRef</li>
 *   <li>本地工作区 diff</li>
 * </ol>
 */
public class GitScanRangeResolver {

    public GitScanRange resolve(Path workingDirectory, StaticScanConfig config) {
        return resolve(workingDirectory, config, new GitPushRange.EnvironmentReader() {
            @Override
            public String get(String name) {
                return System.getenv(name);
            }
        });
    }

    public GitScanRange resolve(Path workingDirectory, StaticScanConfig config,
                                GitPushRange.EnvironmentReader reader) {
        GitPushRange pushRange = GitPushRange.fromEnvironment(reader);
        if (pushRange.isPresent()) {
            return new GitScanRange(
                    "pre-push",
                    pushRange.getBaseRef(),
                    pushRange.getHeadRef(),
                    pushRange.getRemoteBranch(),
                    "",
                    GitDiffResolver.MODE_RANGE);
        }

        String upstreamRef = resolveUpstreamRef(workingDirectory);
        if (!upstreamRef.isEmpty()) {
            String mergeBase = resolveMergeBase(workingDirectory, upstreamRef);
            if (!mergeBase.isEmpty()) {
                return new GitScanRange(
                        "upstream",
                        mergeBase,
                        "HEAD",
                        "",
                        upstreamRef,
                        GitDiffResolver.MODE_RANGE);
            }
        }

        if (config != null && hasExplicitRange(config)) {
            return new GitScanRange(
                    "config",
                    config.getBaseRef(),
                    config.getHeadRef(),
                    "",
                    upstreamRef,
                    GitDiffResolver.MODE_RANGE);
        }

        return new GitScanRange(
                "worktree",
                "",
                "",
                "",
                upstreamRef,
                GitDiffResolver.MODE_WORKTREE);
    }

    private boolean hasExplicitRange(StaticScanConfig config) {
        return !isBlank(config.getBaseRef()) && !isBlank(config.getHeadRef());
    }

    private String resolveUpstreamRef(Path workingDirectory) {
        try {
            String currentBranch = singleLine(runGit(workingDirectory, "branch", "--show-current"));
            if (currentBranch.isEmpty()) {
                return "";
            }
            String remote = singleLine(runGit(workingDirectory, "config", "--get", "branch." + currentBranch + ".remote"));
            String mergeRef = singleLine(runGit(workingDirectory, "config", "--get", "branch." + currentBranch + ".merge"));
            if (remote.isEmpty() || mergeRef.isEmpty()) {
                return "";
            }
            if (mergeRef.startsWith("refs/heads/")) {
                mergeRef = mergeRef.substring("refs/heads/".length());
            }
            return remote + "/" + mergeRef;
        } catch (IOException e) {
            return "";
        }
    }

    private String resolveMergeBase(Path workingDirectory, String upstreamRef) {
        try {
            List<String> output = runGit(workingDirectory, "merge-base", "HEAD", upstreamRef);
            if (output.isEmpty()) {
                return "";
            }
            return output.get(0).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private List<String> runGit(Path workingDirectory, String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String arg : args) {
            command.add(arg);
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null) {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> output = readLines(process);
        int exitCode = waitFor(process);
        if (exitCode != 0) {
            throw new IOException("git 命令执行失败，exitCode=" + exitCode + ", output=" + output);
        }
        return output;
    }

    private String singleLine(List<String> output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        return output.get(0).trim();
    }

    private List<String> readLines(Process process) throws IOException {
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

    private int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 git 命令执行被中断", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
