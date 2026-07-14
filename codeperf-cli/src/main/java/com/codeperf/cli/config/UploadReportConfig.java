package com.codeperf.cli.config;

import lombok.Data;

@Data
public class UploadReportConfig {
    private boolean enabled;
    private String serverUrl;
}
