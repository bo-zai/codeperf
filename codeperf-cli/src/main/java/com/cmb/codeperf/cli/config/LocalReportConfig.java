package com.cmb.codeperf.cli.config;

import lombok.Data;

/**
 * 本地报告配置：控制 JSON/HTML 报告的生成路径和开关。
 * <p>
 * 报告文件：
 * <ul>
 *   <li>JSON 报告：机器可读，用于 CI 集成和后续处理</li>
 *   <li>HTML 报告：人工可读，包含源码片段、调用链、归因信息</li>
 * </ul>
 */
@Data
public class LocalReportConfig {
    private boolean enabled = true;
    private String path = ".codeperf/report/source-report.json";
}

