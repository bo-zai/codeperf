package com.codeperf.analysis.staticanalysis;

import java.util.List;

/**
 * 静态扫描的整体结果。可被 Jackson 序列化为 perf-static.json。
 * 见 docs/05-static-analysis.md 第 5 节。
 */
public class StaticResult {

    private final String targetPackage;
    private final int classesScanned;
    private final List<StaticFinding> findings;

    public StaticResult(String targetPackage, int classesScanned, List<StaticFinding> findings) {
        this.targetPackage = targetPackage;
        this.classesScanned = classesScanned;
        this.findings = findings;
    }

    public String getTargetPackage() { return targetPackage; }
    public int getClassesScanned() { return classesScanned; }
    public List<StaticFinding> getFindings() { return findings; }
}
