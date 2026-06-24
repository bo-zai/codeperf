package com.codeperf.agent.session;

/**
 * 单个 SQL 指纹在一次请求内的聚合记录。
 * count 高 → N+1 嫌疑；maxMs 高 → 慢 SQL。见 docs/02-agent-core.md 第 5.2 节。
 */
public class SqlRecord {

    private String fingerprint;
    private String sampleSql;   // 一条原始 SQL 样例（便于报告展示）
    private int count;
    private long totalMs;
    private long maxMs;
    private boolean slow;       // 是否存在超过慢 SQL 阈值的执行

    public SqlRecord() {
    }

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

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getSampleSql() {
        return sampleSql;
    }

    public void setSampleSql(String sampleSql) {
        this.sampleSql = sampleSql;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTotalMs() {
        return totalMs;
    }

    public void setTotalMs(long totalMs) {
        this.totalMs = totalMs;
    }

    public long getMaxMs() {
        return maxMs;
    }

    public void setMaxMs(long maxMs) {
        this.maxMs = maxMs;
    }

    public boolean isSlow() {
        return slow;
    }

    public void setSlow(boolean slow) {
        this.slow = slow;
    }
}
