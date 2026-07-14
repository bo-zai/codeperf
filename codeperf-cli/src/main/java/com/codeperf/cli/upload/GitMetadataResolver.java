package com.codeperf.cli.upload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class GitMetadataResolver {

    public GitMetadata resolve(Path workingDirectory) {
        return new GitMetadata(
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("rev-parse", "HEAD")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("rev-parse", "--abbrev-ref", "HEAD")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("remote", "get-url", "origin")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("log", "-1", "--format=%an")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("log", "-1", "--format=%ae")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("log", "-1", "--format=%aI")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("log", "-1", "--format=%cn")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("log", "-1", "--format=%ce")),
                runGitOrDefault(workingDirectory, "UNKNOWN", Arrays.asList("log", "-1", "--format=%s")));
    }

    private String runGitOrDefault(Path workingDirectory, String defaultValue, List<String> args) {
        try {
            return runGit(workingDirectory, args);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    private String runGit(Path workingDirectory, List<String> args) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command(args));
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = readFirstLine(process);
        int exitCode = waitFor(process);
        if (exitCode != 0 || output == null || output.trim().isEmpty()) {
            throw new IOException("git metadata command failed");
        }
        return output.trim();
    }

    private List<String> command(List<String> args) {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(args);
        return command;
    }

    private String readFirstLine(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    private int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 git metadata 进程被中断", e);
        }
    }
}
