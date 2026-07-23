package com.cmb.codeperf.cli.config;

import lombok.Data;

/**
 * 报告配置：管理本地报告和远程上传两种输出方式。
 * <p>
 * 输出策略：
 * <ul>
 *   <li>本地报告：默认启用，生成 JSON 和 HTML 报告到 .codeperf/report/</li>
 *   <li>远程上传：默认禁用，需配置 serverUrl，用于 CI 集成和历史趋势分析</li>
 * </ul>
 */
@Data
public class ReportConfig {
    private LocalReportConfig local = new LocalReportConfig();
    private UploadReportConfig upload = new UploadReportConfig();
}

