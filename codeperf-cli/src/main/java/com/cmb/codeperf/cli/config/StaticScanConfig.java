package com.cmb.codeperf.cli.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 静态扫描配置：控制扫描范围、门禁阈值和调用链追踪行为。
 * <p>
 * 关键配置项：
 * <ul>
 *   <li>mode：扫描模式，changed（变更文件）或 all（全量扫描）</li>
 *   <li>failOn：门禁阈值，NONE/INFO/WARN/CRITICAL</li>
 *   <li>callChain：调用链追踪配置，用于检测间接 I/O 调用</li>
 *   <li>ioTypes：关注的 I/O 类型列表，如 mysql、redis、http 等</li>
 * </ul>
 */
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

