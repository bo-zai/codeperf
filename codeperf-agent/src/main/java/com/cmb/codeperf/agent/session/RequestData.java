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

    /** 请求内外部 I/O 语义事件，例如 MyBatis、HTTP 客户端、RPC 调用。 */
    private final List<IoEvent> ioEvents = new ArrayList<>();

    public synchronized void addSample(StackSample s) {
        samples.add(s);
    }

    public SqlRecord sqlRecord(String fingerprint, String sampleSql) {
        return sqls.computeIfAbsent(fingerprint, fp -> new SqlRecord(fp, sampleSql));
    }

    public List<SqlRecord> getSqls() {
        return new ArrayList<>(sqls.values());
    }

    public synchronized void addIoEvent(IoEvent event) {
        IoEvent existing = findSameIoEvent(event);
        if (existing != null) {
            // 循环内重复 I/O 要在请求维度聚合，否则服务端无法判断生产规模放大后的重复调用强度。
            existing.setCount(existing.getCount() + Math.max(event.getCount(), 1));
            existing.setElapsedMs(existing.getElapsedMs() + Math.max(event.getElapsedMs(), 0L));
            return;
        }
        ioEvents.add(event);
    }

    public synchronized List<IoEvent> getIoEvents() {
        return new ArrayList<>(ioEvents);
    }

    private IoEvent findSameIoEvent(IoEvent event) {
        for (IoEvent existing : ioEvents) {
            if (sameEvent(existing, event)) {
                return existing;
            }
        }
        return null;
    }

    private boolean sameEvent(IoEvent left, IoEvent right) {
        return sameText(left.getIoType(), right.getIoType())
                && sameText(left.getFramework(), right.getFramework())
                && sameText(left.getOperation(), right.getOperation())
                && sameText(left.getTarget(), right.getTarget())
                && sameText(left.getMappedStatementId(), right.getMappedStatementId())
                && sameText(left.getClassName(), right.getClassName())
                && sameText(left.getMethodName(), right.getMethodName())
                && sameText(left.getBusinessCallPath(), right.getBusinessCallPath());
    }

    private boolean sameText(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
