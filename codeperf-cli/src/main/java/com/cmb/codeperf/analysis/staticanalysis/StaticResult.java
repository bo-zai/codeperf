package com.cmb.codeperf.analysis.staticanalysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 静态扫描结果：封装目标包、扫描类数和发现列表。
 * <p>
 * 可被 Jackson 序列化为 JSON 文件，用于服务端上传和历史对比。
 * <p>
 * 见 docs/05-static-analysis.md 第 5 节。
 */
@Getter
@AllArgsConstructor
public class StaticResult {

    private final String targetPackage;
    private final int classesScanned;
    private final List<StaticFinding> findings;
}

