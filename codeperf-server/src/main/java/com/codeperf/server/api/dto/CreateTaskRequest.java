package com.codeperf.server.api.dto;

import lombok.Data;

@Data
public class CreateTaskRequest {
    private String project;
    private String commit;
    private String branch;
    private String env;
}
