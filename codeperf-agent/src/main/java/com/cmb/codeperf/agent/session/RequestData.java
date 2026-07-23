package com.cmb.codeperf.agent.session;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次匹配入口的请求所采集到的全部数据。见 docs/02-agent-core.md 第 4 节。
 * 采集期间被单个请求线程独占写入（按 threadId 归并），故内部用普通集合即可；
 * 栈采样由独立线程写入 samples，故 samples 用线程安全集合。
 */
@Getter
@Setter
public class RequestData {

    private String httpMethod;
    private String path;
    private int status;
    private long wallTimeMs;
    private String threadName;
    private long threadId;
    private long allocBytes;

    /** 调用树根（虚拟根，children 为入口直接调用的方法）。 */
    private CallNode callTree = new CallNode("ROOT");

    /** 按 SQL 指纹聚合。 */
    private final Map<String, SqlRecord> sqls = new ConcurrentHashMap<>();

    /** 栈采样快照（由采样线程并发写入）。 */
    private final List<StackSample> samples = new ArrayList<>();

    public synchronized void addSample(StackSample s) {
        samples.add(s);
    }

    public SqlRecord sqlRecord(String fingerprint, String sampleSql) {
        return sqls.computeIfAbsent(fingerprint, fp -> new SqlRecord(fp, sampleSql));
    }

    public List<SqlRecord> getSqls() {
        return new ArrayList<>(sqls.values());
    }
}

