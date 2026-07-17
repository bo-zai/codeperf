package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.source.index.SourceClassIndex;
import com.codeperf.cli.config.StaticScanConfig;
import com.github.javaparser.ast.CompilationUnit;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

/**
 * 源码规则上下文：为规则分析提供必要的数据和配置。
 * <p>
 * 包含：
 * <ul>
 *   <li>units：编译单元列表（通常只有一个）</li>
 *   <li>sourceFile/reportSourceFile：源码文件路径（绝对路径/相对路径）</li>
 *   <li>classIndex：类索引，支持调用链追踪</li>
 *   <li>config：扫描配置，包含调用链深度等参数</li>
 * </ul>
 */
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
