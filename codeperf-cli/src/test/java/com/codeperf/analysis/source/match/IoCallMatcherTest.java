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
    public void should_NotMatchPlainLocalMethod() {
        MethodCallExpr call = StaticJavaParser.parseExpression("formatName(user)")
                .asMethodCallExpr();

        IoMatch match = new IoCallMatcher().match(call, null);

        assertFalse(match.isMatched());
    }
}
