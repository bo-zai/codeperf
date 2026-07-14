package com.codeperf.analysis.source.match;

import com.codeperf.analysis.source.SourceFinding;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IoCallMatcherTest {

    @Test
    public void should_MatchMapperSelectAsDatabaseIo() {
        MethodCallExpr call = StaticJavaParser.parseExpression("orderMapper.selectById(id)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, "OrderMapper");

        assertTrue(match.isMatched());
        assertEquals("DB", match.getIoType());
        assertEquals(SourceFinding.Confidence.HIGH, match.getConfidence());
    }

    @Test
    public void should_MatchRedisTemplateChainAsRedisIo() {
        MethodCallExpr call = StaticJavaParser.parseExpression("redisTemplate.opsForValue().get(key)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, "RedisTemplate");

        assertTrue(match.isMatched());
        assertEquals("REDIS", match.getIoType());
    }

    @Test
    public void should_MatchProjectRedisServiceWrapperAsRedisIo() {
        MethodCallExpr call = StaticJavaParser.parseExpression("redisService.set(key, value, timeout)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, "RedisService");

        assertTrue(match.isMatched());
        assertEquals("REDIS", match.getIoType());
        assertEquals(SourceFinding.Confidence.HIGH, match.getConfidence());
    }

    @Test
    public void should_MatchHutoolHttpUtilAsHttpIo() {
        MethodCallExpr call = StaticJavaParser.parseExpression("HttpUtil.downloadFile(fileUrl, file)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, "HttpUtil");

        assertTrue(match.isMatched());
        assertEquals("HTTP", match.getIoType());
    }

    @Test
    public void should_MatchProjectHttpUtilsWrapperAsHttpIo() {
        MethodCallExpr call = StaticJavaParser.parseExpression("HttpUtils.putPay(payMap, url)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, "HttpUtils");

        assertTrue(match.isMatched());
        assertEquals("HTTP", match.getIoType());
    }

    @Test
    public void should_NotMatchPlainLocalMethod() {
        MethodCallExpr call = StaticJavaParser.parseExpression("formatName(user)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, null);

        assertFalse(match.isMatched());
    }
}
