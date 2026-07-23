package com.cmb.codeperf.analysis.staticanalysis;

import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRuleConfig;
import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRuleContext;
import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRuleRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 静态扫描入口：遍历 classes 目录下的 .class 文件，用 ASM 解析后运行静态规则。
 * <p>
 * 扫描流程：
 * <ol>
 *   <li>遍历 classes 目录下属于 targetPackage 的 .class 文件</li>
 *   <li>使用 BytecodeAnalyzer 解析字节码，提取方法、循环、调用点、分配点</li>
 *   <li>运行 StaticRuleRegistry 中的所有静态规则</li>
 *   <li>返回 StaticResult，包含扫描类数和发现列表</li>
 * </ol>
 * <p>
 * 与源码扫描的区别：
 * <ul>
 *   <li>源码扫描（SourceScanner）：基于 AST，精度高，支持调用链追踪</li>
 *   <li>字节码扫描（StaticScanner）：基于 ASM，无需源码，适合编译后检查</li>
 * </ul>
 * <p>
 * 见 docs/05-static-analysis.md 第 5 节。
 */
public class StaticScanner {

    private final StaticRuleRegistry registry;

    public StaticScanner() {
        this(StaticRuleRegistry.defaultRegistry());
    }

    public StaticScanner(StaticRuleRegistry registry) {
        this.registry = registry;
    }

    public StaticResult scan(File classesDir, String targetPackage) throws IOException {
        return scan(classesDir, targetPackage, Collections.emptyList());
    }

    public StaticResult scan(File classesDir, String targetPackage, List<String> sourceRoots) throws IOException {
        // 将包名转换为内部路径格式（如 com.example → com/example）
        String pkgInternal = targetPackage == null ? "" : targetPackage.replace('.', '/');
        Path root = classesDir.toPath();

        List<ClassAnalysis> classes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            // 过滤 .class 文件：仅处理目标包下的类
            List<Path> classFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> {
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        return pkgInternal.isEmpty() || rel.startsWith(pkgInternal);
                    })
                    .collect(Collectors.toList());

            // 解析字节码：提取方法、循环、调用点、分配点
            for (Path p : classFiles) {
                byte[] bytes = Files.readAllBytes(p);
                try {
                    ClassAnalysis analysis = BytecodeAnalyzer.analyze(bytes);
                    // 源文件路径用于报告中的位置引用
                    analysis.setSourceFile(resolveSourceFile(analysis, sourceRoots));
                    classes.add(analysis);
                } catch (RuntimeException ex) {
                    // 解析失败不阻断扫描，记录错误并跳过
                    System.err.println("[codeperf] 跳过无法解析的类: " + p + " (" + ex.getMessage() + ")");
                }
            }
        }

        // 执行所有静态规则，收集发现
        List<StaticFinding> findings = registry.run(new StaticRuleContext(
                classes, targetPackage, sourceRoots, StaticRuleConfig.empty()));

        return new StaticResult(targetPackage, classes.size(), findings);
    }

    private String resolveSourceFile(ClassAnalysis analysis, List<String> sourceRoots) {
        if (analysis == null) {
            return null;
        }
        String relative = analysis.getClassName().replace('.', '/') + ".java";
        if (sourceRoots != null) {
            for (String root : sourceRoots) {
                if (root == null || root.trim().isEmpty()) {
                    continue;
                }
                File candidate = new File(root, relative);
                if (candidate.isFile()) {
                    return candidate.getPath();
                }
            }
        }
        return relative;
    }
}

