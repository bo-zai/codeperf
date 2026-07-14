package com.codeperf.cli.config;

import lombok.Data;

@Data
public class LocalReportConfig {
    private boolean enabled = true;
    private String path = ".codeperf/report/source-report.json";
}
