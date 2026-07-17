package com.codeperf.cli.report;

import com.codeperf.analysis.source.SourceFinding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * HTML 报告渲染辅助类：提供安全转义、路径处理、源码片段读取等工具方法。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>XSS 防护：所有输出到 HTML 的内容都经过转义</li>
 *   <li>路径处理：统一使用正斜杠，生成合法的 HTML 元素 ID</li>
 *   <li>源码片段：只读取上下文几行，避免内存占用</li>
 * </ul>
 */
final class SourceScanHtmlSupport {

    private static final int SOURCE_CONTEXT_LINES = 3;

    private SourceScanHtmlSupport() {
    }

    static List<SourceLine> readSourceSnippet(SourceFinding finding, Path projectRoot) {
        Path source = projectRoot.resolve(finding.getSourceFile()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            return Collections.emptyList();
        }
        try {
            List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
            int start = Math.max(1, finding.getLineNumber() - SOURCE_CONTEXT_LINES);
            int end = Math.min(lines.size(), finding.getLineNumber() + SOURCE_CONTEXT_LINES);
            List<SourceLine> snippet = new ArrayList<>();
            for (int lineNumber = start; lineNumber <= end; lineNumber++) {
                snippet.add(new SourceLine(lineNumber, lines.get(lineNumber - 1)));
            }
            return snippet;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    static String fileAnchor(String sourceFile) {
        String value = sourceFile == null ? "unknown" : sourceFile;
        StringBuilder anchor = new StringBuilder("file-");
        boolean previousDash = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                anchor.append(ch);
                previousDash = false;
            } else if (!previousDash) {
                anchor.append('-');
                previousDash = true;
            }
        }
        while (anchor.length() > 5 && anchor.charAt(anchor.length() - 1) == '-') {
            anchor.deleteCharAt(anchor.length() - 1);
        }
        return anchor.toString();
    }

    static String shortFileName(String sourceFile) {
        if (isBlank(sourceFile)) {
            return "unknown";
        }
        int slash = Math.max(sourceFile.lastIndexOf('/'), sourceFile.lastIndexOf('\\'));
        return slash >= 0 ? sourceFile.substring(slash + 1) : sourceFile;
    }

    static String shortLocation(SourceFinding finding) {
        return shortFileName(finding.getSourceFile()) + ":" + finding.getLineNumber();
    }

    static String location(SourceFinding finding) {
        return finding.getSourceFile() + ":" + finding.getLineNumber();
    }

    static String valueOrUnknown(String value) {
        if (isBlank(value)) {
            return "unknown";
        }
        return value;
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    static String escapeAttribute(String value) {
        return escape(value).replace("\n", " ").replace("\r", " ");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static class SourceLine {
        private final int lineNumber;
        private final String content;

        SourceLine(int lineNumber, String content) {
            this.lineNumber = lineNumber;
            this.content = content;
        }

        int getLineNumber() {
            return lineNumber;
        }

        String getContent() {
            return content;
        }
    }

    static class IndexedFinding {
        final int index;
        final SourceFinding finding;

        IndexedFinding(int index, SourceFinding finding) {
            this.index = index;
            this.finding = finding;
        }
    }
}
