package com.codeperf.cli.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StaticScanConfig {
    private boolean enabled = true;
    private String mode = "changed";
    private List<String> sourceRoots = new ArrayList<>();
    private boolean includeTests = false;
    private String baseRef = "origin/master";
    private String headRef = "HEAD";
    private String failOn = "WARN";
    private CallChainConfig callChain = new CallChainConfig();
    private List<String> ioTypes = new ArrayList<>();

    public StaticScanConfig() {
        sourceRoots.add("src/main/java");
        ioTypes.add("mysql");
        ioTypes.add("mongodb");
        ioTypes.add("redis");
        ioTypes.add("gaussdb");
        ioTypes.add("http");
        ioTypes.add("rpc");
        ioTypes.add("sdk");
    }
}
