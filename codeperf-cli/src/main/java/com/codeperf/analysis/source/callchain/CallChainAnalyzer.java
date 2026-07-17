package com.codeperf.analysis.source.callchain;

import com.codeperf.analysis.source.CallChainStep;
import com.codeperf.analysis.source.index.IndexedMethod;
import com.codeperf.analysis.source.index.SourceClassIndex;
import com.codeperf.analysis.source.match.IoCallMatcher;
import com.codeperf.analysis.source.match.IoMatch;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 调用链分析器：从方法调用出发递归追踪直到找到 I/O 调用。
 * <p>
 * 追踪流程：
 * <ol>
 *   <li>从循环内的方法调用开始</li>
 *   <li>在类索引中查找被调用方法的声明</li>
 *   <li>检查方法体内是否有 I/O 调用（直接匹配）</li>
 *   <li>若无直接 I/O，递归追踪方法内的其他方法调用</li>
 *   <li>记录调用链路径，直到达到最大深度或找到 I/O</li>
 * </ol>
 * <p>
 * 设计约束：
 * <ul>
 *   <li>深度限制：避免无限递归（默认 maxDepth=2）</li>
 *   <li>访问记录：防止循环调用导致重复追踪</li>
 *   <li>仅追踪同类：简化跨类调用分析，实际场景中同类调用占多数</li>
 * </ul>
 */
public class CallChainAnalyzer {

    private final SourceClassIndex index;
    private final IoCallMatcher matcher;

    public CallChainAnalyzer(SourceClassIndex index, IoCallMatcher matcher) {
        this.index = index;
        this.matcher = matcher;
    }

    public Optional<Result> findIo(String className, MethodDeclaration caller,
                                   MethodCallExpr call, int maxDepth) {
        List<CallChainStep> chain = new ArrayList<>();
        chain.add(step(className, caller));
        Set<String> visited = new HashSet<>();
        return findIo(className, call.getNameAsString(), maxDepth, chain, visited);
    }

    private Optional<Result> findIo(String className, String methodName, int depth,
                                    List<CallChainStep> chain, Set<String> visited) {
        // 深度检查：防止无限递归
        if (depth <= 0) {
            return Optional.empty();
        }

        // 访问检查：防止循环调用重复追踪
        String key = className + "#" + methodName;
        if (!visited.add(key)) {
            return Optional.empty();
        }

        // 在类索引中查找方法声明
        Optional<IndexedMethod> method = index.findMethod(className, methodName);
        if (!method.isPresent()) {
            return Optional.empty();
        }
        chain.add(step(className, method.get().getDeclaration()));

        // 遍历方法体内的所有方法调用
        for (MethodCallExpr nested : method.get().getDeclaration().findAll(MethodCallExpr.class)) {
            // 尝试推断接收者类型（用于 I/O 匹配）
            String receiverType = nested.getScope().isPresent()
                    ? index.findFieldType(className, nested.getScope().get().toString()).orElse(null)
                    : null;

            // 直接 I/O 匹配检查
            IoMatch match = matcher.match(nested, receiverType);
            if (match.isMatched()) {
                return Optional.of(new Result(match, nested, new ArrayList<>(chain)));
            }

            // 递归追踪：无显式接收者的方法调用（同类方法）
            if (!nested.getScope().isPresent()) {
                Optional<Result> nestedResult = findIo(className, nested.getNameAsString(),
                        depth - 1, chain, visited);
                if (nestedResult.isPresent()) {
                    return nestedResult;
                }
            }
        }
        chain.remove(chain.size() - 1);
        return Optional.empty();
    }

    private CallChainStep step(String className, MethodDeclaration method) {
        int line = method.getBegin().isPresent() ? method.getBegin().get().line : 0;
        return new CallChainStep(className, method.getNameAsString(), "", line);
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private final IoMatch match;
        private final MethodCallExpr ioCall;
        private final List<CallChainStep> callChain;
    }
}
