package com.codeperf.analysis.source;

import com.codeperf.analysis.source.index.SourceClassIndex;
import com.codeperf.analysis.source.rule.SourceRule;
import com.codeperf.analysis.source.rule.SourceRuleContext;
import com.codeperf.analysis.source.rule.SourceRuleRegistry;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 源码扫描入口：解析 Java 文件 → 构建索引 → 执行规则 → 去重 → 返回扫描结果。
 * <p>
 * 扫描流程：
 * <ol>
 *   <li>使用 JavaParser 解析所有源码文件，构建类索引</li>
 *   <li>遍历规则注册表中的所有规则，对每个文件执行分析</li>
 *   <li>对发现去重（相同规则、文件、行号、I/O 类型、证据、调用链）</li>
 *   <li>返回扫描结果，包含扫描文件数、发现列表、解析错误</li>
 * </ol>
 * <p>
 * 设计决策：
 * <ul>
 *   <li>索引优先：先构建类索引，支持跨方法调用链追踪</li>
 *   <li>去重策略：保留最高严重度和置信度的发现</li>
 *   <li>容错处理：解析错误不阻断扫描，记录在结果中</li>
 * </ul>
 */
public class SourceScanner {

    private final JavaAstParser parser;
    private final SourceRuleRegistry registry;

    public SourceScanner() {
        this(new JavaAstParser(), SourceRuleRegistry.defaultRegistry());
    }

    public SourceScanner(JavaAstParser parser, SourceRuleRegistry registry) {
        this.parser = parser;
        this.registry = registry;
    }

    public SourceScanResult scan(SourceScanRequest request) {
        List<ParsedSource> parsedSources = new ArrayList<>();
        SourceClassIndex index = new SourceClassIndex();
        List<String> parseErrors = new ArrayList<>();

        // 先构建类索引再执行规则：支持跨方法调用链追踪，避免规则执行时重复解析
        for (Path file : request.getSourceFiles()) {
            try {
                CompilationUnit unit = parser.parse(file);
                parsedSources.add(new ParsedSource(file, unit));
                index.add(file, unit);
            } catch (IOException e) {
                parseErrors.add(e.getMessage());
            }
        }

        List<SourceFinding> findings = new ArrayList<>();
        for (ParsedSource parsed : parsedSources) {
            SourceRuleContext context = new SourceRuleContext(
                    Collections.singletonList(parsed.getUnit()),
                    parsed.getFile(),
                    reportSourceFile(request.getRootDirectory(), parsed.getFile()),
                    index,
                    request.getConfig());
            for (SourceRule rule : registry.rules()) {
                findings.addAll(rule.analyze(context));
            }
        }

        // 去重策略：保留最高严重度和置信度的发现，避免同一风险多次报告
        return new SourceScanResult(parsedSources.size(), deduplicate(findings), parseErrors);
    }

    private List<SourceFinding> deduplicate(List<SourceFinding> findings) {
        // 使用 LinkedHashMap 保持发现顺序
        Map<String, SourceFinding> unique = new LinkedHashMap<>();
        for (SourceFinding finding : findings) {
            String key = deduplicationKey(finding);
            SourceFinding existing = unique.get(key);
            // 保留更高质量的发现（更高严重度或置信度）
            if (existing == null || isHigherQuality(finding, existing)) {
                unique.put(key, finding);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private String reportSourceFile(Path rootDirectory, Path sourceFile) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        if (rootDirectory != null) {
            Path normalizedRoot = rootDirectory.toAbsolutePath().normalize();
            if (normalizedSource.startsWith(normalizedRoot)) {
                return normalizedRoot.relativize(normalizedSource).toString().replace('\\', '/');
            }
        }
        return normalizedSource.getFileName() == null
                ? normalizedSource.toString().replace('\\', '/')
                : normalizedSource.getFileName().toString().replace('\\', '/');
    }

    private String deduplicationKey(SourceFinding finding) {
        // 去重键：规则 + 文件 + 行号 + 循环范围 + I/O 类型 + 证据 + 调用链
        // 相同键的发现视为重复，仅保留质量最高的
        return finding.getRuleId()
                + "|" + finding.getSourceFile()
                + "|" + finding.getLineNumber()
                + "|" + finding.getLoopStartLine()
                + "|" + finding.getLoopEndLine()
                + "|" + finding.getIoType()
                + "|" + finding.getEvidence()
                + "|" + callChainKey(finding);
    }

    private String callChainKey(SourceFinding finding) {
        if (finding.getCallChain() == null || finding.getCallChain().isEmpty()) {
            return "";
        }
        return finding.getCallChain().stream()
                .map(step -> step.getClassName()
                        + "#" + step.getMethodName()
                        + "@" + step.getFilePath()
                        + ":" + step.getLineNumber())
                .collect(Collectors.joining(">"));
    }

    private boolean isHigherQuality(SourceFinding candidate, SourceFinding existing) {
        if (candidate.getSeverity().getLevel() != existing.getSeverity().getLevel()) {
            return candidate.getSeverity().getLevel() > existing.getSeverity().getLevel();
        }
        return candidate.getConfidence().ordinal() > existing.getConfidence().ordinal();
    }

    private static class ParsedSource {
        private final Path file;
        private final CompilationUnit unit;

        private ParsedSource(Path file, CompilationUnit unit) {
            this.file = file;
            this.unit = unit;
        }

        private Path getFile() {
            return file;
        }

        private CompilationUnit getUnit() {
            return unit;
        }
    }
}
