package com.codeperf.cli.config;

import lombok.Data;

@Data
public class CallChainConfig {
    private boolean enabled = true;
    private int maxDepth = 2;
}
