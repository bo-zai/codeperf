package com.cmb.codeperf.cli.attribution;

import com.cmb.codeperf.analysis.source.RiskAttribution;
import com.cmb.codeperf.analysis.source.SourceFinding;
import com.cmb.codeperf.analysis.source.SourceScanResult;
import com.cmb.codeperf.cli.git.ChangedLineSet;
import com.cmb.codeperf.cli.git.GitChangedLineResolver;
import com.cmb.codeperf.cli.git.GitDiffResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 风险归因增强器：对发现执行 git blame，标记 NEW/MODIFIED/HISTORICAL 归因和提交人信息。
 * <p>
 * 归因逻辑：
 * <ul>
 *   <li>NEW/MODIFIED：发现的 I/O 行或循环起始行在变更范围内，通过 git blame 获取提交人</li>
 *   <li>HISTORICAL：发现不在变更范围内，标记为历史遗留问题，仍记录原始提交人</li>
 * </ul>
 * <p>
 * 设计意图：
 * <ul>
 *   <li>区分新增风险和历史风险：门禁仅阻断新增风险，历史风险仅报告不阻断</li>
 *   <li>责任追溯：记录提交人和提交信息，便于问题分配和审查</li>
 * </ul>
 */
public class GitRiskAttributionEnricher {

    public SourceScanResult enrich(SourceScanResult result, Path workingDirectory,
                                   String base, String head, String diffMode) {
        // 解析变更行集合：git diff 输出解析失败时返回空集合，不阻断扫描流程
        ChangedLineSet changedLines = resolveChangedLines(workingDirectory, base, head, diffMode);
        List<SourceFinding> enriched = new ArrayList<>();

        // 遍历所有发现，执行归因
        for (SourceFinding finding : result.getFindings()) {
            // 检查发现的多个关键行是否在变更范围内：I/O 行、循环调用行、循环起始行都可能是引入风险的变更点
            boolean changed = changedLines.containsAny(
                    finding.getSourceFile(),
                    finding.getIoLine(),
                    finding.getLoopCallLine(),
                    finding.getLoopStartLine());

            // 归因判定：变更范围内的风险标记为 NEW（阻断），否则标记为 HISTORICAL（不阻断）
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

