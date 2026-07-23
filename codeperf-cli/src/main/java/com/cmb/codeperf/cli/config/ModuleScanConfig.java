package com.cmb.codeperf.cli.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 模块扫描配置：为多模块项目定制扫描范围和规则。
 * <p>
 * 使用场景：当项目包含多个独立模块（如 user-service、order-service）时，
 * 可为每个模块配置独立的 sourceRoots 和 targetPackages，便于模块级统计和报告。
 * <p>
 * 示例配置：
 * <pre>
 * modules:
 *   - name: user-service
 *     sourceRoots: [user-service/src/main/java]
 *     targetPackages: [com.example.user]
 * </pre>
 */
@Data
public class ModuleScanConfig {
    private String name;
    private List<String> sourceRoots = new ArrayList<>();
    private List<String> targetPackages = new ArrayList<>();
}

