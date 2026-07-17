package com.codeperf.cli.module;

import com.codeperf.cli.config.ModuleScanConfig;
import com.codeperf.cli.config.StaticScanConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模块解析器：根据配置的 sourceRoots 解析源码文件所属模块名称。
 * <p>
 * 解析规则：
 * <ol>
 *   <li>优先匹配模块配置中最长的 sourceRoot 前缀</li>
 *   <li>未匹配到模块时，提取路径首段作为模块名（如 user-service/src/... → user-service）</li>
 *   <li>首段为 "src" 时，归入 root 模块（单模块项目）</li>
 * </ol>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>报告统计：按模块聚合风险数量</li>
 *   <li>多模块项目：区分 user-service、order-service 等模块</li>
 * </ul>
 */
public class SourceModuleResolver {

    private static final String DEFAULT_MODULE = "root";

    private final List<ModuleScanConfig> modules;

    public SourceModuleResolver(List<ModuleScanConfig> modules) {
        this.modules = modules == null ? Collections.<ModuleScanConfig>emptyList() : modules;
    }

    /**
     * 返回静态扫描实际使用的源码根目录。
     *
     * @param staticScan 全局静态扫描配置
     * @param modules 模块配置
     * @return 配置后的源码根目录
     */
    public static List<String> effectiveSourceRoots(StaticScanConfig staticScan, List<ModuleScanConfig> modules) {
        List<String> roots = new ArrayList<>();
        if (modules != null) {
            for (ModuleScanConfig module : modules) {
                if (module == null || module.getSourceRoots() == null) {
                    continue;
                }
                roots.addAll(module.getSourceRoots());
            }
        }
        if (!roots.isEmpty()) {
            return roots;
        }
        return staticScan.getSourceRoots();
    }

    /**
     * 解析源码文件所属模块。
     *
     * @param sourceFile 相对 Git 根目录的源码路径
     * @return 模块名称
     */
    public String resolveModuleName(String sourceFile) {
        String normalizedSourceFile = normalize(sourceFile);
        ModuleScanConfig matched = null;
        String matchedRoot = "";
        // 遍历所有模块配置，匹配最长的 sourceRoot 前缀（最长匹配原则）
        for (ModuleScanConfig module : modules) {
            if (module == null || module.getSourceRoots() == null) {
                continue;
            }
            for (String sourceRoot : module.getSourceRoots()) {
                String normalizedRoot = normalize(sourceRoot);
                if (matchesRoot(normalizedSourceFile, normalizedRoot) && normalizedRoot.length() > matchedRoot.length()) {
                    matched = module;
                    matchedRoot = normalizedRoot;
                }
            }
        }
        // 匹配成功则返回模块名，否则推断路径首段作为模块名
        if (matched != null && !isBlank(matched.getName())) {
            return matched.getName();
        }
        return inferFirstPathSegment(normalizedSourceFile);
    }

    private boolean matchesRoot(String sourceFile, String sourceRoot) {
        return sourceFile.equals(sourceRoot) || sourceFile.startsWith(sourceRoot + "/");
    }

    private String inferFirstPathSegment(String sourceFile) {
        if (isBlank(sourceFile)) {
            return DEFAULT_MODULE;
        }
        int slash = sourceFile.indexOf('/');
        if (slash <= 0) {
            return DEFAULT_MODULE;
        }
        String firstSegment = sourceFile.substring(0, slash);
        return "src".equals(firstSegment) ? DEFAULT_MODULE : firstSegment;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\\', '/').trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
