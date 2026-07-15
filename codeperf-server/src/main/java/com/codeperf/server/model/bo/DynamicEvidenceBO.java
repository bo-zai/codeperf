package com.codeperf.server.model.bo;

import lombok.Data;

@Data
public class DynamicEvidenceBO {
    private String taskId;
    private String env;
    private String appName;
    private String entryKey;
    private String rawPayload;
}
