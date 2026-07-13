package com.codeperf.agent.session;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次采集会话的顶层数据，最终被 SessionWriter 序列化为 JSON。
 * 见 docs/02-agent-core.md 第 4、8 节。
 */
@Getter
@Setter
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
}
