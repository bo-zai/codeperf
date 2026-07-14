package com.codeperf.analysis.source;

import com.codeperf.analysis.Severity;
import lombok.Getter;

import java.util.List;

@Getter
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
    private final String loopMethodName;
    private final int loopCallLine;
    private final int ioLine;

    public SourceFinding(String type, Severity severity, Confidence confidence,
                         String description, String evidence, String sourceFile,
                         int lineNumber, int loopStartLine, int loopEndLine,
                         String ioType, List<CallChainStep> callChain) {
        this(type, severity, confidence, description, evidence, sourceFile,
                lineNumber, loopStartLine, loopEndLine, ioType, callChain, "", lineNumber, lineNumber);
    }

    public SourceFinding(String type, Severity severity, Confidence confidence,
                         String description, String evidence, String sourceFile,
                         int lineNumber, int loopStartLine, int loopEndLine,
                         String ioType, List<CallChainStep> callChain,
                         String loopMethodName, int loopCallLine, int ioLine) {
        this.type = type;
        this.severity = severity;
        this.confidence = confidence;
        this.description = description;
        this.evidence = evidence;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.loopStartLine = loopStartLine;
        this.loopEndLine = loopEndLine;
        this.ioType = ioType;
        this.callChain = callChain;
        this.loopMethodName = loopMethodName;
        this.loopCallLine = loopCallLine;
        this.ioLine = ioLine;
    }
}
