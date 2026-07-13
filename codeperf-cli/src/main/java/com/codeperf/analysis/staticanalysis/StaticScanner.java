package com.codeperf.analysis.staticanalysis;

import com.codeperf.analysis.staticanalysis.rule.StaticRuleConfig;
import com.codeperf.analysis.staticanalysis.rule.StaticRuleContext;
import com.codeperf.analysis.staticanalysis.rule.StaticRuleRegistry;

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
 * 静态扫描入口：遍历 classes 目录下属于 targetPackage 的 .class，
 * 用 ASM 解析后运行全部静态规则，汇总为 {@link StaticResult}。
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
        String pkgInternal = targetPackage == null ? "" : targetPackage.replace('.', '/');
        Path root = classesDir.toPath();

        List<ClassAnalysis> classes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> classFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> {
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        return pkgInternal.isEmpty() || rel.startsWith(pkgInternal);
                    })
                    .collect(Collectors.toList());

            for (Path p : classFiles) {
                byte[] bytes = Files.readAllBytes(p);
                try {
                    ClassAnalysis analysis = BytecodeAnalyzer.analyze(bytes);
                    analysis.setSourceFile(resolveSourceFile(analysis, sourceRoots));
                    classes.add(analysis);
                } catch (RuntimeException ex) {
                    System.err.println("[codeperf] 跳过无法解析的类: " + p + " (" + ex.getMessage() + ")");
                }
            }
        }

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
