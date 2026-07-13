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
import java.util.List;

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
                    Collections.singletonList(parsed.getUnit()), parsed.getFile(), index, request.getConfig());
            for (SourceRule rule : registry.rules()) {
                findings.addAll(rule.analyze(context));
            }
        }
        return new SourceScanResult(parsedSources.size(), findings, parseErrors);
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
