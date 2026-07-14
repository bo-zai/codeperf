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
    private final String remoteUrl;
    private final String authorName;
    private final String authorEmail;
    private final String authorTime;
    private final String committerName;
    private final String committerEmail;
    private final String commitMessage;
    private final String reportJson;
}
