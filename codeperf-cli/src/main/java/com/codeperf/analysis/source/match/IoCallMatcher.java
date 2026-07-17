package com.codeperf.analysis.source.match;

import com.codeperf.analysis.source.SourceFinding;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Locale;

/**
 * I/O 调用匹配器：识别方法调用是否为外部 I/O 操作（DB/Redis/MongoDB/HTTP/RPC/SDK）。
 * <p>
 * 匹配规则：
 * <ul>
 *   <li>Redis：接收者为 redisTemplate、stringRedisTemplate、RedissonClient 等</li>
 *   <li>MongoDB：接收者为 mongoTemplate、MongoRepository 等</li>
 *   <li>HTTP：接收者为 RestTemplate、WebClient、OkHttpClient、Feign 等</li>
 *   <li>DB：接收者以 mapper/repository/dao 结尾，方法以 select/query/find 等开头</li>
 *   <li>SDK：接收者以 client/gateway/facade 结尾，方法为 query/get/call 等</li>
 * </ul>
 * <p>
 * 设计要点：
 * <ul>
 *   <li>启发式匹配：基于常见命名约定，可能存在误报</li>
 *   <li>置信度：HIGH（明确框架类）vs MEDIUM（通用命名）</li>
 * </ul>
 */
public class IoCallMatcher {

    public IoMatch match(MethodCallExpr call, String receiverType) {
        // 统一转小写，便于匹配（忽略大小写差异）
        String callText = call.toString().toLowerCase(Locale.ROOT);
        String receiver = receiverName(call).toLowerCase(Locale.ROOT);
        String type = receiverType == null ? "" : receiverType.toLowerCase(Locale.ROOT);
        String method = call.getNameAsString().toLowerCase(Locale.ROOT);

        // Redis 匹配：Spring RedisTemplate、Redisson 等主流客户端
        if ((containsAny(receiver, type, "redistemplate", "stringredistemplate", "redissonclient", "redisservice")
                || callText.contains("redistemplate.") || callText.contains("opsforvalue().get"))
                && startsWithAny(method, "get", "set", "del", "delete", "increment", "decrement", "expire")) {
            return new IoMatch(true, "REDIS", SourceFinding.Confidence.HIGH,
                    "Redis 客户端调用: " + call);
        }
        // MongoDB 匹配：Spring MongoTemplate、MongoRepository
        if (containsAny(receiver, type, "mongotemplate", "mongorepository")) {
            return new IoMatch(true, "MONGO", SourceFinding.Confidence.HIGH,
                    "Mongo 客户端调用: " + call);
        }
        // HTTP 匹配：RestTemplate、WebClient、OkHttp、Feign 等主流客户端
        if (containsAny(receiver, type, "resttemplate", "webclient", "okhttpclient", "httpclient", "feign",
                "httputil", "httputils", "okhttputil")
                && startsWithAny(method, "get", "post", "put", "delete", "execute", "download")) {
            return new IoMatch(true, "HTTP", SourceFinding.Confidence.HIGH,
                    "HTTP 客户端调用: " + call);
        }
        // DB 匹配：MyBatis Mapper、JPA Repository、通用 DAO 命名
        if (containsAny(receiver, type, "mapper", "repository", "dao")
                && startsWithAny(method, "select", "query", "find", "get", "list", "insert", "update", "delete")) {
            return new IoMatch(true, "DB", SourceFinding.Confidence.HIGH,
                    "数据库访问调用: " + call);
        }
        // SDK 匹配：通用客户端命名（置信度较低，需人工确认）
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
