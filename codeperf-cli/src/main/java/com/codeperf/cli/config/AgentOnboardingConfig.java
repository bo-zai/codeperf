package com.codeperf.cli.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentOnboardingConfig {
    private boolean enabled = true;
    private String serverUrl = "http://codeperf.company.com";
    private String configPath = ".codeperf/agent.yml";
    private String jarPath = "/opt/codeperf/codeperf-agent.jar";
    private List<String> targetPackages = new ArrayList<>();
}
