package com.codeperf.cli.config;

import lombok.Data;

@Data
public class ReportConfig {
    private LocalReportConfig local = new LocalReportConfig();
    private UploadReportConfig upload = new UploadReportConfig();
}
