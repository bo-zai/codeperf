package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.source.index.SourceClassIndex;
import com.codeperf.cli.config.StaticScanConfig;
import com.github.javaparser.ast.CompilationUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
@AllArgsConstructor
public class SourceRuleContext {
    private final List<CompilationUnit> units;
    private final Path sourceFile;
    private final SourceClassIndex classIndex;
    private final StaticScanConfig config;
}
