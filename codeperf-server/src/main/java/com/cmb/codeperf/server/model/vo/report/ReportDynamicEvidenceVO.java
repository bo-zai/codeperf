package com.cmb.codeperf.server.model.vo.report;

import lombok.Data;

/**
 * 综合报告中的动态运行证据。
 */
@Data
public class ReportDynamicEvidenceVO {

    /** 应用名称 */
    private String appName;

    /** 运行环境 */
    private String env;

    /** 入口接口或方法 */
    private String entryKey;

    /** 原始上报内容 */
    private String rawPayload;
}
