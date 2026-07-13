package com.codeperf.analysis.source.match;

import com.codeperf.analysis.source.SourceFinding;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Locale;

public class IoCallMatcher {

    public IoMatch match(MethodCallExpr call, String receiverType) {
        String callText = call.toString().toLowerCase(Locale.ROOT);
        String receiver = receiverName(call).toLowerCase(Locale.ROOT);
        String type = receiverType == null ? "" : receiverType.toLowerCase(Locale.ROOT);
        String method = call.getNameAsString().toLowerCase(Locale.ROOT);

        if (containsAny(receiver, type, "redistemplate", "stringredistemplate", "redissonclient")
                || callText.contains("redistemplate.") || callText.contains("opsforvalue().get")) {
            return new IoMatch(true, "REDIS", SourceFinding.Confidence.HIGH,
                    "Redis 客户端调用: " + call);
        }
        if (containsAny(receiver, type, "mongotemplate", "mongorepository")) {
            return new IoMatch(true, "MONGO", SourceFinding.Confidence.HIGH,
                    "Mongo 客户端调用: " + call);
        }
        if (containsAny(receiver, type, "resttemplate", "webclient", "okhttpclient", "httpclient", "feign")) {
            return new IoMatch(true, "HTTP", SourceFinding.Confidence.HIGH,
                    "HTTP 客户端调用: " + call);
        }
        if (containsAny(receiver, type, "mapper", "repository", "dao")
                && startsWithAny(method, "select", "query", "find", "get", "list", "insert", "update", "delete")) {
            return new IoMatch(true, "DB", SourceFinding.Confidence.HIGH,
                    "数据库访问调用: " + call);
        }
        if (containsAny(receiver, type, "client", "gateway", "facade")
                && startsWithAny(method, "query", "get", "call", "execute", "invoke", "send")) {
            return new IoMatch(true, "SDK", SourceFinding.Confidence.MEDIUM,
                    "外部客户端调用: " + call);
        }
        return IoMatch.none();
    }

    private String receiverName(MethodCallExpr call) {
        if (!call.getScope().isPresent()) {
            return "";
        }
        Expression scope = call.getScope().get();
        if (scope.isMethodCallExpr()) {
            return receiverName(scope.asMethodCallExpr());
        }
        return scope.toString();
    }

    private boolean containsAny(String first, String second, String... patterns) {
        for (String pattern : patterns) {
            if (first.contains(pattern) || second.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
