package com.codeperf.analysis.staticanalysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 静态扫描的整体结果。可被 Jackson 序列化为 perf-static.json。
 * 见 docs/05-static-analysis.md 第 5 节。
 */
@Getter
@AllArgsConstructor
public class StaticResult {

    private final String targetPackage;
    private final int classesScanned;
    private final List<StaticFinding> findings;
}
