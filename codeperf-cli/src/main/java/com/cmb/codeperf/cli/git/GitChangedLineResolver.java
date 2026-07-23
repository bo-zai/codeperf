package com.cmb.codeperf.cli.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变更行解析器：解析 git diff 输出，获取新增/修改的行号集合。
 * <p>
 * 用途：风险归因时判断发现是否属于变更范围（NEW/MODIFIED vs HISTORICAL）。
 * <p>
 * 实现要点：
 * <ul>
 *   <li>解析 {@code @@ -n +m @@} hunk 头部，追踪新文件行号</li>
 *   <li>仅记录 {@code +} 开头的行（新增内容），忽略 {@code -} 开头的行（删除内容）</li>
 *   <li>使用 {@code --unified=0} 减少上下文行，提升解析精度</li>
 * </ul>
 */
public class GitChangedLineResolver {
    private static final Pattern HUNK = Pattern.compile("@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,(\\d+))? @@.*");

    public ChangedLineSet resolve(Path workingDirectory, String base, String head, String diffMode) throws IOException {
        return parse(runGitDiff(workingDirectory, base, head, diffMode));
    }

    ChangedLineSet parse(List<String> diffLines) {
        ChangedLineSet result = new ChangedLineSet();
        String currentFile = "";
        int newLine = 0;
        for (String line : diffLines) {
            // 解析文件路径：从 "+++ b/path" 提取相对路径
            if (line.startsWith("+++ b/")) {
                currentFile = line.substring("+++ b/".length()).replace('\\', '/');
                continue;
            }
            // 解析 hunk 头部：提取新文件起始行号，用于追踪变更行
            Matcher matcher = HUNK.matcher(line);
            if (matcher.matches()) {
                newLine = Integer.parseInt(matcher.group(1));
                continue;
            }
            if (currentFile.isEmpty() || line.startsWith("+++") || line.startsWith("---")) {
                continue;
            }
            // 仅记录新增行（+），忽略删除行（-）和上下文行（空格）
            if (line.startsWith("+")) {
                result.add(currentFile, newLine);
                newLine++;
            } else if (line.startsWith("-")) {
                continue;
            } else if (line.startsWith(" ")) {
                newLine++;
            }
        }
        return result;
    }

    private List<String> runGitDiff(Path workingDirectory, String base, String head, String diffMode) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(buildCommand(base, head, diffMode));
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> output = readLines(process);
        int exitCode = waitFor(process);
        if (exitCode != 0) {
            throw new IOException("git diff changed line 执行失败，exitCode=" + exitCode + ", output=" + output);
        }
        return output;
    }

    private List<String> buildCommand(String base, String head, String diffMode) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("diff");
        command.add("--unified=0");
        if (GitDiffResolver.MODE_STAGED.equalsIgnoreCase(diffMode)) {
            command.add("--cached");
            return command;
        }
        if (GitDiffResolver.MODE_WORKTREE.equalsIgnoreCase(diffMode)) {
            command.add("HEAD");
            return command;
        }
        command.add(base);
        command.add(head);
        return command;
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
            throw new IOException("等待 git diff changed line 进程被中断", e);
        }
    }
}

