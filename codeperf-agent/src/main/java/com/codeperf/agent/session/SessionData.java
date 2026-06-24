package com.codeperf.agent.session;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次采集会话的顶层数据，最终被 SessionWriter 序列化为 JSON。
 * 见 docs/02-agent-core.md 第 4、8 节。
 */
public class SessionData {

    private String entryMethod;
    private String entryPath;
    private List<String> targetPackages;
    private long startTimeEpochMs;
    private String javaVersion;
    private final List<RequestData> requests = new ArrayList<>();

    public void addRequest(RequestData r) {
        requests.add(r);
    }

    public String getEntryMethod() {
        return entryMethod;
    }

    public void setEntryMethod(String entryMethod) {
        this.entryMethod = entryMethod;
    }

    public String getEntryPath() {
        return entryPath;
    }

    public void setEntryPath(String entryPath) {
        this.entryPath = entryPath;
    }

    public List<String> getTargetPackages() {
        return targetPackages;
    }

    public void setTargetPackages(List<String> targetPackages) {
        this.targetPackages = targetPackages;
    }

    public long getStartTimeEpochMs() {
        return startTimeEpochMs;
    }

    public void setStartTimeEpochMs(long startTimeEpochMs) {
        this.startTimeEpochMs = startTimeEpochMs;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public List<RequestData> getRequests() {
        return requests;
    }
}
