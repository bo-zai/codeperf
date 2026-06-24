package com.codeperf.agent.session;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法调用树节点。selfTimeMs = totalTimeMs - 子节点 totalTimeMs 之和。
 * 见 docs/02-agent-core.md 第 4 节。
 */
public class CallNode {

    private String method;
    private int count;
    private long totalTimeMs;
    private long selfTimeMs;
    private final List<CallNode> children = new ArrayList<>();

    // 序列化用无参构造
    public CallNode() {
    }

    public CallNode(String method) {
        this.method = method;
    }

    /** 在 children 中查找同名节点，没有则新建。用于聚合同一调用路径的多次调用。 */
    public CallNode childFor(String childMethod) {
        for (CallNode c : children) {
            if (c.method.equals(childMethod)) {
                return c;
            }
        }
        CallNode c = new CallNode(childMethod);
        children.add(c);
        return c;
    }

    public void accumulate(long totalMs, long childMs) {
        this.count++;
        this.totalTimeMs += totalMs;
        this.selfTimeMs += (totalMs - childMs);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public long getSelfTimeMs() {
        return selfTimeMs;
    }

    public void setSelfTimeMs(long selfTimeMs) {
        this.selfTimeMs = selfTimeMs;
    }

    public List<CallNode> getChildren() {
        return children;
    }
}
