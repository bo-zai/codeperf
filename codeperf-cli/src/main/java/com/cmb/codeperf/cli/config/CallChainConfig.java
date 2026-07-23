package com.cmb.codeperf.cli.config;

import lombok.Data;

/**
 * 调用链追踪配置：控制从方法调用到 I/O 调用的链路追踪深度。
 * <p>
 * 设计决策：
 * <ul>
 *   <li>深度限制：避免无限递归，默认 maxDepth=2 可覆盖大多数间接调用场景</li>
 *   <li>性能权衡：深度越大，检测越准确，但扫描时间越长</li>
 * </ul>
 */
@Data
public class CallChainConfig {
    private boolean enabled = true;
    private int maxDepth = 2;
}

