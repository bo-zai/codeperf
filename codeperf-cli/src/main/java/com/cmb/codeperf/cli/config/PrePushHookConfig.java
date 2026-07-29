package com.cmb.codeperf.cli.config;

import lombok.Data;

/**
 * pre-push 钩子策略配置。
 */
@Data
public class PrePushHookConfig {

    private boolean blockOnFailure = false;
}
