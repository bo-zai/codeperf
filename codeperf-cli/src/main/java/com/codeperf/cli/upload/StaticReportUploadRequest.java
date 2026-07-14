package com.codeperf.cli.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StaticReportUploadRequest {
    private final String project;
    private final String env;
    private final String commit;
    private final String branch;
    private final String reportJson;
}
