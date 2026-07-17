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

/**
 * 循环 I/O 放大规则：检测循环内的直接/间接 I/O 调用。
 * <p>
 * 检测逻辑：
 * <ol>
 *   <li>遍历所有 for/while/do-while/for-each 循环</li>
 *   <li>检查循环体内的方法调用是否为 I/O 调用（直接匹配）</li>
 *   <li>若启用调用链追踪，递归检查方法调用是否间接触发 I/O</li>
 * </ol>
 * <p>
 * 设计要点：
 * <ul>
 *   <li>归属于当前循环：避免嵌套循环重复检测</li>
 *   <li>置信度：直接 I/O 为 HIGH，间接 I/O 取决于调用链分析结果</li>
 *   <li>调用链：记录从循环方法到 I/O 调用的完整路径</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public class LoopIoAmplificationAstRule implements SourceRule {

    private static final String RULE_ID = "LOOP_IO_AMPLIFICATION";

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

        // 遍历循环体内的所有方法调用
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            // 归属于当前循环检查：避免嵌套循环中内层循环的调用被外层重复检测
            if (!belongsToCurrentLoop(call, loop)) {
                continue;
            }

            // 尝试推断接收者类型（用于 I/O 匹配）
            String receiverType = call.getScope().isPresent()
                    ? context.getClassIndex().findFieldType(className, call.getScope().get().toString()).orElse(null)
                    : null;

            // 直接 I/O 匹配检查
            IoMatch match = matcher.match(call, receiverType);
            if (match.isMatched()) {
                findings.add(toFinding(loop, call, call, context, match, Collections.<CallChainStep>emptyList()));
                continue;
            }

            // 间接 I/O 调用链追踪：仅对无显式接收者的方法调用（同类方法），避免跨类追踪复杂度
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
        // 向上遍历 AST 节点，检查是否在当前循环体内
        // 嵌套循环场景：内层循环的调用不应被外层循环重复检测
        Optional<Node> current = call.getParentNode();
        while (current.isPresent()) {
            Node node = current.get();
            if (isLoop(node)) {
                // 遇到的第一个循环就是直接包含该调用的循环
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
                RULE_ID,
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
