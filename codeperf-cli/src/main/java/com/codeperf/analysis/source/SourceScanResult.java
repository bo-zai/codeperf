package com.codeperf.analysis.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SourceScanResult {
    private final int filesScanned;
    private final List<SourceFinding> findings;
    private final List<String> parseErrors;
}
