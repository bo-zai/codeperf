package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.source.CallChainStep;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.callchain.CallChainAnalyzer;
import com.codeperf.analysis.source.match.IoCallMatcher;
import com.codeperf.analysis.source.match.IoMatch;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class LoopIoAmplificationAstRule implements SourceRule {

    private final IoCallMatcher matcher = new IoCallMatcher();

    @Override
    public List<SourceFinding> analyze(SourceRuleContext context) {
        List<SourceFinding> findings = new ArrayList<>();
        for (com.github.javaparser.ast.CompilationUnit unit : context.getUnits()) {
            for (ForStmt loop : unit.findAll(ForStmt.class)) {
                analyzeLoop(loop, loop.getBody(), context, findings);
            }
            for (ForEachStmt loop : unit.findAll(ForEachStmt.class)) {
                analyzeLoop(loop, loop.getBody(), context, findings);
            }
            for (WhileStmt loop : unit.findAll(WhileStmt.class)) {
                analyzeLoop(loop, loop.getBody(), context, findings);
            }
            for (DoStmt loop : unit.findAll(DoStmt.class)) {
                analyzeLoop(loop, loop.getBody(), context, findings);
            }
        }
        return findings;
    }

    private void analyzeLoop(Node loop, Statement body, SourceRuleContext context, List<SourceFinding> findings) {
        String className = findClassName(loop);
        CallChainAnalyzer analyzer = new CallChainAnalyzer(context.getClassIndex(), matcher);
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            String receiverType = call.getScope().isPresent()
                    ? context.getClassIndex().findFieldType(className, call.getScope().get().toString()).orElse(null)
                    : null;
            IoMatch match = matcher.match(call, receiverType);
            if (match.isMatched()) {
                findings.add(toFinding(loop, call, context, match, Collections.<CallChainStep>emptyList()));
                continue;
            }
            if (!call.getScope().isPresent() && context.getConfig().getCallChain().isEnabled()) {
                MethodDeclaration caller = call.findAncestor(MethodDeclaration.class).orElse(null);
                if (caller != null) {
                    Optional<CallChainAnalyzer.Result> indirect = analyzer.findIo(
                            className, caller, call, context.getConfig().getCallChain().getMaxDepth());
                    if (indirect.isPresent()) {
                        findings.add(toFinding(loop, indirect.get().getIoCall(), context,
                                indirect.get().getMatch(), indirect.get().getCallChain()));
                    }
                }
            }
        }
    }

    private SourceFinding toFinding(Node loop, MethodCallExpr call, SourceRuleContext context,
                                    IoMatch match, List<CallChainStep> callChain) {
        int callLine = call.getBegin().isPresent() ? call.getBegin().get().line : 0;
        int loopStart = loop.getBegin().isPresent() ? loop.getBegin().get().line : 0;
        int loopEnd = loop.getEnd().isPresent() ? loop.getEnd().get().line : 0;
        String sourceFile = context.getSourceFile().toString().replace('\\', '/');
        return new SourceFinding(
                "Loop I/O Amplification",
                Severity.WARN,
                match.getConfidence(),
                "循环体内存在外部 I/O 调用，生产数据量放大时可能导致接口响应变慢。",
                match.getReason(),
                sourceFile,
                callLine,
                loopStart,
                loopEnd,
                match.getIoType(),
                callChain);
    }

    private String findClassName(Node node) {
        Optional<ClassOrInterfaceDeclaration> clazz = node.findAncestor(ClassOrInterfaceDeclaration.class);
        return clazz.isPresent() ? clazz.get().getNameAsString() : "";
    }
}
