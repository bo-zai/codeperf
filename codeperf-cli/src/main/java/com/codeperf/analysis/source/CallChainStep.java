package com.codeperf.analysis.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调用链步骤：表示从循环方法到 I/O 调用之间的一步方法调用。
 * <p>
 * 用途：
 * <ul>
 *   <li>展示间接 I/O 调用路径：循环 → 方法 A → 方法 B → I/O</li>
 *   <li>帮助开发者理解 I/O 调用的来源，便于定位和修复</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public class CallChainStep {
    private final String className;
    private final String methodName;
    private final String filePath;
    private final int lineNumber;
}
