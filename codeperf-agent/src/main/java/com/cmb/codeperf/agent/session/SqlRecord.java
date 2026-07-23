package com.cmb.codeperf.agent.session;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单个 SQL 指纹在一次请求内的聚合记录。
 * count 高 → N+1 嫌疑；maxMs 高 → 慢 SQL。见 docs/02-agent-core.md 第 5.2 节。
 */
@Getter
@Setter
@NoArgsConstructor
public class SqlRecord {

    private String fingerprint;
    private String sampleSql;   // 一条原始 SQL 样例（便于报告展示）
    private int count;
    private long totalMs;
    private long maxMs;
    private boolean slow;       // 是否存在超过慢 SQL 阈值的执行

    public SqlRecord(String fingerprint, String sampleSql) {
        this.fingerprint = fingerprint;
        this.sampleSql = sampleSql;
    }

    public void addExecution(long ms, boolean isSlow) {
        this.count++;
        this.totalMs += ms;
        if (ms > this.maxMs) {
            this.maxMs = ms;
        }
        if (isSlow) {
            this.slow = true;
        }
    }
}

