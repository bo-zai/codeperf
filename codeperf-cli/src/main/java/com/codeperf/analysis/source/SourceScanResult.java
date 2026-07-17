package com.codeperf.analysis.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 扫描结果：封装扫描文件数、发现列表和解析错误列表。
 * <p>
 * 字段说明：
 * <ul>
 *   <li>filesScanned：成功解析的文件数</li>
 *   <li>findings：检测到的风险列表</li>
 *   <li>parseErrors：解析失败的文件错误信息</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public class SourceScanResult {
    private final int filesScanned;
    private final List<SourceFinding> findings;
    private final List<String> parseErrors;
}
