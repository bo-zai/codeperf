package com.cmb.codeperf.agent.collect;

import java.util.regex.Pattern;

/**
 * SQL 指纹归一化：把字面量替换为占位符，便于识别"同一条 SQL 高频执行"（N+1）。
 * 见 docs/02-agent-core.md 第 5.2 节。
 *
 * 例：SELECT * FROM orders WHERE user_id = 12   ->   select * from orders where user_id = ?
 *     SELECT * FROM orders WHERE user_id = 34   ->   （同指纹）
 */
public final class SqlFingerprint {

    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:[^']|'')*'");
    private static final Pattern NUMERIC_LITERAL = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern IN_LIST = Pattern.compile("in\\s*\\(\\s*\\?(?:\\s*,\\s*\\?)*\\s*\\)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SqlFingerprint() {
    }

    public static String of(String sql) {
        if (sql == null) {
            return "";
        }
        String s = sql.toLowerCase();
        s = STRING_LITERAL.matcher(s).replaceAll("?");
        s = NUMERIC_LITERAL.matcher(s).replaceAll("?");
        s = WHITESPACE.matcher(s).replaceAll(" ").trim();
        s = IN_LIST.matcher(s).replaceAll("in (?)");
        return s;
    }
}

