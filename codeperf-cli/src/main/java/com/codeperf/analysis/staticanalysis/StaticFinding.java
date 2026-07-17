package com.codeperf.analysis.staticanalysis;

import com.codeperf.analysis.Severity;
import lombok.Getter;

/**
 * 静态发现：表示字节码分析检测到的性能风险。
 * <p>
 * 与源码发现（SourceFinding）的区别：
 * <ul>
 *   <li>基于字节码而非源码 AST</li>
 *   <li>包含 classMethod（来源方法）而非 sourceFile</li>
 *   <li>额外包含 callOwner、callName 等字节码级别的调用信息</li>
 * </ul>
 * <p>
 * 见 docs/05-static-analysis.md 第 3 节。
 */
@Getter
public class StaticFinding {

    public enum Confidence { LOW, MEDIUM, HIGH }

    private final String type;
    private final Severity severity;
    private final Confidence confidence;
    private final String description;
    private final String evidence;
    private final String classMethod; // 来源方法，如 com.demo.Service.method
    private final String sourceFile;
    private final int lineNumber;
    private final int loopStartLine;
    private final int loopEndLine;
    private final String className;
    private final String methodName;
    private final String callOwner;
    private final String callName;
    private final String ioType;

    public StaticFinding(String type, Severity severity, Confidence confidence,
                         String description, String evidence, String classMethod) {
        this(type, severity, confidence, description, evidence, classMethod,
                null, 0, 0, 0, null, null, null, null, null);
    }

    public StaticFinding(String type, Severity severity, Confidence confidence,
                         String description, String evidence, String classMethod,
                         String sourceFile, int lineNumber, int loopStartLine, int loopEndLine,
                         String className, String methodName, String callOwner, String callName,
                         String ioType) {
        this.type = type;
        this.severity = severity;
        this.confidence = confidence;
        this.description = description;
        this.evidence = evidence;
        this.classMethod = classMethod;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.loopStartLine = loopStartLine;
        this.loopEndLine = loopEndLine;
        this.className = className;
        this.methodName = methodName;
        this.callOwner = callOwner;
        this.callName = callName;
        this.ioType = ioType;
    }

}
