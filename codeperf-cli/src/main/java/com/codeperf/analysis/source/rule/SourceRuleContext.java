package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.source.index.SourceClassIndex;
import com.codeperf.cli.config.StaticScanConfig;
import com.github.javaparser.ast.CompilationUnit;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
public class SourceRuleContext {
    private final List<CompilationUnit> units;
    private final Path sourceFile;
    private final String reportSourceFile;
    private final SourceClassIndex classIndex;
    private final StaticScanConfig config;

    public SourceRuleContext(List<CompilationUnit> units, Path sourceFile, String reportSourceFile,
                             SourceClassIndex classIndex, StaticScanConfig config) {
        this.units = units;
        this.sourceFile = sourceFile;
        this.reportSourceFile = reportSourceFile;
        this.classIndex = classIndex;
        this.config = config;
    }

    public SourceRuleContext(List<CompilationUnit> units, Path sourceFile,
                             SourceClassIndex classIndex, StaticScanConfig config) {
        this(units, sourceFile, sourceFile.toString().replace('\\', '/'), classIndex, config);
    }
}
