package com.cmb.codeperf.server.model.bo;

import lombok.Data;

/**
 * 动态请求级证据业务对象。
 */
@Data
public class DynamicRequestEvidenceBO {

    private Long id;
    private String taskId;
    private Long rawEvidenceId;
    private String env;
    private String appName;
    private String entryMethod;
    private String entryPath;
    private String entryKey;
    private long wallTimeMs;
    private String rawPayload;
}
