package com.codeperf.agent.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次匹配入口的请求所采集到的全部数据。见 docs/02-agent-core.md 第 4 节。
 * 采集期间被单个请求线程独占写入（按 threadId 归并），故内部用普通集合即可；
 * 栈采样由独立线程写入 samples，故 samples 用线程安全集合。
 */
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

    // ===== getters / setters（含序列化用）=====

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getWallTimeMs() {
        return wallTimeMs;
    }

    public void setWallTimeMs(long wallTimeMs) {
        this.wallTimeMs = wallTimeMs;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getAllocBytes() {
        return allocBytes;
    }

    public void setAllocBytes(long allocBytes) {
        this.allocBytes = allocBytes;
    }

    public CallNode getCallTree() {
        return callTree;
    }

    public void setCallTree(CallNode callTree) {
        this.callTree = callTree;
    }

    public List<SqlRecord> getSqls() {
        return new ArrayList<>(sqls.values());
    }

    public List<StackSample> getSamples() {
        return samples;
    }
}
