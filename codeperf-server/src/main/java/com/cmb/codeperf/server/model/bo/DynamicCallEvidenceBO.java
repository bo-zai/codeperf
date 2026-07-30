package com.cmb.codeperf.server.model.bo;

import lombok.Data;

/**
 * 动态调用级证据业务对象。
 */
@Data
public class DynamicCallEvidenceBO {

    private Long id;
    private String taskId;
    private Long requestEvidenceId;
    private Long rawEvidenceId;
    private String entryKey;
    private String className;
    private String methodName;
    private String fullMethodName;
    private String callPath;
    private int callCount;
    private long totalTimeMs;
    private String rawPayload;
}
