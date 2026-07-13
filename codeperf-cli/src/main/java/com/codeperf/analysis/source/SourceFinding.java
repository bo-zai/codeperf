package com.codeperf.analysis.source;

import com.codeperf.analysis.Severity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SourceFinding {
    public enum Confidence { LOW, MEDIUM, HIGH }

    private final String type;
    private final Severity severity;
    private final Confidence confidence;
    private final String description;
    private final String evidence;
    private final String sourceFile;
    private final int lineNumber;
    private final int loopStartLine;
    private final int loopEndLine;
    private final String ioType;
    private final List<CallChainStep> callChain;
}
