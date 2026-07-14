package com.codeperf.server.domain.model;

import lombok.Data;

@Data
public class DynamicEvidenceRecord {
    private String taskId;
    private String env;
    private String appName;
    private String entryKey;
    private String rawPayload;
}
