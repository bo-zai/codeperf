package com.codeperf.analysis.staticanalysis;

import com.codeperf.analysis.staticanalysis.rules.HeavyComputeSuspect;
import com.codeperf.analysis.staticanalysis.rules.LargeAllocSuspect;
import com.codeperf.analysis.staticanalysis.rules.NPlusOneSuspect;
import com.codeperf.analysis.staticanalysis.rules.NSquaredSuspect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 静态扫描入口：遍历 classes 目录下属于 targetPackage 的 .class，
 * 用 ASM 解析后运行全部静态规则，汇总为 {@link StaticResult}。
 * 见 docs/05-static-analysis.md 第 5 节。
 */
public class StaticScanner {

    private final List<BytecodeRule> rules = Arrays.asList(
            new NPlusOneSuspect(),
            new NSquaredSuspect(),
            new HeavyComputeSuspect(),
            new LargeAllocSuspect()
    );

    public StaticResult scan(File classesDir, String targetPackage) throws IOException {
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
                    classes.add(BytecodeAnalyzer.analyze(bytes));
                } catch (RuntimeException ex) {
                    System.err.println("[codeperf] 跳过无法解析的类: " + p + " (" + ex.getMessage() + ")");
                }
            }
        }

        List<StaticFinding> findings = new ArrayList<>();
        for (BytecodeRule rule : rules) {
            findings.addAll(rule.analyze(classes, targetPackage));
        }

        return new StaticResult(targetPackage, classes.size(), findings);
    }
}
