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
            if (!belongsToCurrentLoop(call, loop)) {
                continue;
            }
            String receiverType = call.getScope().isPresent()
                    ? context.getClassIndex().findFieldType(className, call.getScope().get().toString()).orElse(null)
                    : null;
            IoMatch match = matcher.match(call, receiverType);
            if (match.isMatched()) {
                findings.add(toFinding(loop, call, call, context, match, Collections.<CallChainStep>emptyList()));
                continue;
            }
            if (!call.getScope().isPresent() && context.getConfig().getCallChain().isEnabled()) {
                MethodDeclaration caller = call.findAncestor(MethodDeclaration.class).orElse(null);
                if (caller != null) {
                    Optional<CallChainAnalyzer.Result> indirect = analyzer.findIo(
                            className, caller, call, context.getConfig().getCallChain().getMaxDepth());
                    if (indirect.isPresent()) {
                        findings.add(toFinding(loop, call, indirect.get().getIoCall(), context,
                                indirect.get().getMatch(), indirect.get().getCallChain()));
                    }
                }
            }
        }
    }

    private boolean belongsToCurrentLoop(MethodCallExpr call, Node currentLoop) {
        Optional<Node> current = call.getParentNode();
        while (current.isPresent()) {
            Node node = current.get();
            if (isLoop(node)) {
                return node == currentLoop;
            }
            current = node.getParentNode();
        }
        return false;
    }

    private boolean isLoop(Node node) {
        return node instanceof ForStmt
                || node instanceof ForEachStmt
                || node instanceof WhileStmt
                || node instanceof DoStmt;
    }

    private SourceFinding toFinding(Node loop, MethodCallExpr loopCall, MethodCallExpr ioCall, SourceRuleContext context,
                                    IoMatch match, List<CallChainStep> callChain) {
        int loopCallLine = loopCall.getBegin().isPresent() ? loopCall.getBegin().get().line : 0;
        int ioLine = ioCall.getBegin().isPresent() ? ioCall.getBegin().get().line : 0;
        int loopStart = loop.getBegin().isPresent() ? loop.getBegin().get().line : 0;
        int loopEnd = loop.getEnd().isPresent() ? loop.getEnd().get().line : 0;
        String sourceFile = context.getReportSourceFile();
        return new SourceFinding(
                "Loop I/O Amplification",
                Severity.WARN,
                match.getConfidence(),
                "循环体内存在外部 I/O 调用，生产数据量放大时可能导致接口响应变慢。",
                match.getReason(),
                sourceFile,
                ioLine,
                loopStart,
                loopEnd,
                match.getIoType(),
                callChain,
                findMethodName(loop),
                loopCallLine,
                ioLine);
    }

    private String findClassName(Node node) {
        Optional<ClassOrInterfaceDeclaration> clazz = node.findAncestor(ClassOrInterfaceDeclaration.class);
        return clazz.isPresent() ? clazz.get().getNameAsString() : "";
    }

    private String findMethodName(Node node) {
        Optional<MethodDeclaration> method = node.findAncestor(MethodDeclaration.class);
        return method.isPresent() ? method.get().getNameAsString() : "";
    }
}
