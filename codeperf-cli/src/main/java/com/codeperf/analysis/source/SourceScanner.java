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
        return new SourceScanResult(parsedSources.size(), deduplicate(findings), parseErrors);
    }

    private List<SourceFinding> deduplicate(List<SourceFinding> findings) {
        Map<String, SourceFinding> unique = new LinkedHashMap<>();
        for (SourceFinding finding : findings) {
            String key = deduplicationKey(finding);
            SourceFinding existing = unique.get(key);
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
