package com.cmb.codeperf.cli.config;

import lombok.Data;

/**
 * 上传报告配置：控制扫描报告上传到 CodePerf 服务端的开关和地址。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>CI 集成：每次构建上传报告，建立历史趋势</li>
 *   <li>团队协作：在 Web 界面审查报告，分配问题负责人</li>
 * </ul>
 */
@Data
public class UploadReportConfig {
    private boolean enabled;
    private String serverUrl;
}

