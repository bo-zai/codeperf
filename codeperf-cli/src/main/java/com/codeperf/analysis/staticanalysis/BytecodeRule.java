package com.codeperf.analysis.staticanalysis;

import java.util.List;

/**
 * 静态规则接口。每条规则从 ClassAnalysis 列表中提取 StaticFinding。
 */
public interface BytecodeRule {
    List<StaticFinding> analyze(List<ClassAnalysis> classes, String targetPackage);
}
