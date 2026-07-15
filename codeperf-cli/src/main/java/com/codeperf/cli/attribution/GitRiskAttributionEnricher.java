package com.codeperf.cli.attribution;

import com.codeperf.analysis.source.RiskAttribution;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.SourceScanResult;
import com.codeperf.cli.git.ChangedLineSet;
import com.codeperf.cli.git.GitChangedLineResolver;
import com.codeperf.cli.git.GitDiffResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitRiskAttributionEnricher {

    public SourceScanResult enrich(SourceScanResult result, Path workingDirectory,
                                   String base, String head, String diffMode) {
        ChangedLineSet changedLines = resolveChangedLines(workingDirectory, base, head, diffMode);
        List<SourceFinding> enriched = new ArrayList<>();
        for (SourceFinding finding : result.getFindings()) {
            boolean changed = changedLines.containsAny(
                    finding.getSourceFile(),
                    finding.getIoLine(),
                    finding.getLoopCallLine(),
                    finding.getLoopStartLine());
            RiskAttribution attribution = changed
                    ? blame(workingDirectory, finding)
                    : historical(workingDirectory, finding);
            enriched.add(finding.withAttribution(attribution));
        }
        return new SourceScanResult(result.getFilesScanned(), enriched, result.getParseErrors());
    }

    private ChangedLineSet resolveChangedLines(Path workingDirectory, String base, String head, String diffMode) {
        try {
            return new GitChangedLineResolver().resolve(workingDirectory, base, head, diffMode);
        } catch (IOException e) {
            return new ChangedLineSet();
        }
    }

    private RiskAttribution blame(Path workingDirectory, SourceFinding finding) {
        int line = finding.getIoLine() > 0 ? finding.getIoLine() : finding.getLineNumber();
        try {
            BlameInfo info = runBlame(workingDirectory, finding.getSourceFile(), line);
            return new RiskAttribution(
                    RiskAttribution.RiskScope.NEW,
                    true,
                    RiskAttribution.AttributionConfidence.HIGH,
                    info.author,
                    info.authorEmail,
                    info.commit,
                    info.authorTime,
                    info.summary);
        } catch (IOException e) {
            return new RiskAttribution(
                    RiskAttribution.RiskScope.NEW,
                    true,
                    RiskAttribution.AttributionConfidence.LOW,
                    "",
                    "",
                    "",
                    "",
                    "");
        }
    }

    private RiskAttribution historical(Path workingDirectory, SourceFinding finding) {
        int line = finding.getIoLine() > 0 ? finding.getIoLine() : finding.getLineNumber();
        try {
            BlameInfo info = runBlame(workingDirectory, finding.getSourceFile(), line);
            return new RiskAttribution(
                    RiskAttribution.RiskScope.HISTORICAL,
                    false,
                    RiskAttribution.AttributionConfidence.HIGH,
                    info.author,
                    info.authorEmail,
                    info.commit,
                    info.authorTime,
                    info.summary);
        } catch (IOException e) {
            return new RiskAttribution(
                    RiskAttribution.RiskScope.HISTORICAL,
                    false,
                    RiskAttribution.AttributionConfidence.LOW,
                    "",
                    "",
                    "",
                    "",
                    "");
        }
    }

    private BlameInfo runBlame(Path workingDirectory, String file, int line) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("blame");
        command.add("--line-porcelain");
        command.add("-L");
        command.add(line + "," + line);
        command.add("HEAD");
        command.add("--");
        command.add(file);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> output = readLines(process);
        int exitCode = waitFor(process);
        if (exitCode != 0 || output.isEmpty()) {
            throw new IOException("git blame 执行失败，exitCode=" + exitCode + ", output=" + output);
        }
        return parseBlame(output);
    }

    private BlameInfo parseBlame(List<String> output) {
        BlameInfo info = new BlameInfo();
        String first = output.get(0);
        int space = first.indexOf(' ');
        info.commit = space > 0 ? first.substring(0, space) : first;
        for (String line : output) {
            if (line.startsWith("author ")) {
                info.author = line.substring("author ".length());
            } else if (line.startsWith("author-mail ")) {
                info.authorEmail = trimEmail(line.substring("author-mail ".length()));
            } else if (line.startsWith("author-time ")) {
                info.authorTime = line.substring("author-time ".length());
            } else if (line.startsWith("summary ")) {
                info.summary = line.substring("summary ".length());
            }
        }
        return info;
    }

    private String trimEmail(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
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
            throw new IOException("等待 git blame 进程被中断", e);
        }
    }

    private static class BlameInfo {
        private String commit = "";
        private String author = "";
        private String authorEmail = "";
        private String authorTime = "";
        private String summary = "";
    }
}
