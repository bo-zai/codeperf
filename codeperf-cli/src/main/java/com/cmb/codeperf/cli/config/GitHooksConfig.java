package com.cmb.codeperf.cli.config;

import lombok.Data;

/**
 * Git Hook 配置。
 * 默认不阻断 push，避免工具试点期影响研发交付；需要强管控时由仓库配置显式开启。
 */
@Data
public class GitHooksConfig {

    private PrePushHookConfig prePush = new PrePushHookConfig();
}
