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
        if (depth <= 0) {
            return Optional.empty();
        }
        String key = className + "#" + methodName;
        if (!visited.add(key)) {
            return Optional.empty();
        }
        Optional<IndexedMethod> method = index.findMethod(className, methodName);
        if (!method.isPresent()) {
            return Optional.empty();
        }
        chain.add(step(className, method.get().getDeclaration()));
        for (MethodCallExpr nested : method.get().getDeclaration().findAll(MethodCallExpr.class)) {
            String receiverType = nested.getScope().isPresent()
                    ? index.findFieldType(className, nested.getScope().get().toString()).orElse(null)
                    : null;
            IoMatch match = matcher.match(nested, receiverType);
            if (match.isMatched()) {
                return Optional.of(new Result(match, nested, new ArrayList<>(chain)));
            }
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
