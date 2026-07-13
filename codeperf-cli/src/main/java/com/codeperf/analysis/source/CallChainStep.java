package com.codeperf.analysis.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CallChainStep {
    private final String className;
    private final String methodName;
    private final String filePath;
    private final int lineNumber;
}
