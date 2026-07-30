package com.cmb.codeperf.server.model.bo;

import lombok.Data;

/**
 * 静态风险与动态运行证据的佐证关系业务对象。
 */
@Data
public class StaticDynamicCorroborationBO {

    private Long id;
    private String taskId;
    private Long staticFindingId;
    private Long findingIssueId;
    private String status;
    private int hitRequestCount;
    private int hitEntryCount;
    private int maxRepeatCount;
    private int avgRepeatCount;
    private String topEntryKey;
    private String reason;
}
