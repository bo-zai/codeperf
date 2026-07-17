package com.codeperf.analysis.source;

import com.codeperf.analysis.Severity;
import lombok.Getter;

import java.util.List;

/**
 * 源码发现：表示一个检测到的性能风险。
 * <p>
 * 核心字段：
 * <ul>
 *   <li>ruleId：规则标识，如 "loop-io-amplification"</li>
 *   <li>severity：严重度（INFO/WARN/CRITICAL）</li>
 *   <li>confidence：置信度（LOW/MEDIUM/HIGH）</li>
 *   <li>sourceFile + lineNumber：风险位置</li>
 *   <li>loopStartLine/loopEndLine：循环范围</li>
 *   <li>ioType：I/O 类型（DB/Redis/HTTP/RPC/SDK）</li>
 *   <li>callChain：从循环到 I/O 的调用链</li>
 *   <li>attribution：风险归因（NEW/MODIFIED/HISTORICAL）</li>
 * </ul>
 */
@Getter
public class SourceFinding {
    public enum Confidence { LOW, MEDIUM, HIGH }

    private final String ruleId;
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
    private final RiskAttribution attribution;

    public SourceFinding(String ruleId, Severity severity, Confidence confidence,
                         String description, String evidence, String sourceFile,
                         int lineNumber, int loopStartLine, int loopEndLine,
                         String ioType, List<CallChainStep> callChain) {
        this(ruleId, severity, confidence, description, evidence, sourceFile,
                lineNumber, loopStartLine, loopEndLine, ioType, callChain,
                "", lineNumber, lineNumber, RiskAttribution.unknown());
    }

    public SourceFinding(String ruleId, Severity severity, Confidence confidence,
                         String description, String evidence, String sourceFile,
                         int lineNumber, int loopStartLine, int loopEndLine,
                         String ioType, List<CallChainStep> callChain,
                         String loopMethodName, int loopCallLine, int ioLine) {
        this(ruleId, severity, confidence, description, evidence, sourceFile, lineNumber,
                loopStartLine, loopEndLine, ioType, callChain, loopMethodName, loopCallLine,
                ioLine, RiskAttribution.unknown());
    }

    public SourceFinding(String ruleId, Severity severity, Confidence confidence,
                         String description, String evidence, String sourceFile,
                         int lineNumber, int loopStartLine, int loopEndLine,
                         String ioType, List<CallChainStep> callChain,
                         String loopMethodName, int loopCallLine, int ioLine,
                         RiskAttribution attribution) {
        this.ruleId = ruleId;
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
        this.attribution = attribution == null ? RiskAttribution.unknown() : attribution;
    }

    public SourceFinding withAttribution(RiskAttribution attribution) {
        return new SourceFinding(
                ruleId,
                severity,
                confidence,
                description,
                evidence,
                sourceFile,
                lineNumber,
                loopStartLine,
                loopEndLine,
                ioType,
                callChain,
                loopMethodName,
                loopCallLine,
                ioLine,
                attribution);
    }
}
