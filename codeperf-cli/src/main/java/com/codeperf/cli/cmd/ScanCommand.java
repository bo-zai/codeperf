package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.HtmlReport;
import com.codeperf.analysis.staticanalysis.ClasspathResolver;
import com.codeperf.analysis.staticanalysis.StaticResult;
import com.codeperf.analysis.staticanalysis.StaticScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * scan 子命令：静态扫描已编译的 .class，产出 perf-static.json（可选 HTML）。
 * 见 docs/05-static-analysis.md 第 6 节。
 */
@Parameters(commandDescription = "Static bytecode scan for performance suspects (no app launch needed)")
public class ScanCommand {

    @Parameter(names = "--target-package", description = "Application package prefix to scan", required = true)
    private String targetPackage;

    @Parameter(names = "--classes-dir", description = "Compiled classes dir (auto-detected if omitted)")
    private String classesDir;

    @Parameter(names = "--output", description = "Static result JSON output")
    private String output = "perf-static.json";

    @Parameter(names = "--report", description = "If set, also write a static-only HTML report to this path")
    private String report;

    public int execute() {
        File dir = ClasspathResolver.resolve(classesDir);
        if (dir == null) {
            System.err.println("[codeperf] 未找到编译产物目录。请用 --classes-dir 指定 (如 target/classes)。");
            return 2;
        }
        System.out.println("[codeperf] 扫描目录: " + dir.getPath());
        System.out.println("[codeperf] 目标包: " + targetPackage);

        try {
            StaticScanner scanner = new StaticScanner();
            StaticResult result = scanner.scan(dir, targetPackage);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(result);
            Files.write(Paths.get(output), json.getBytes(StandardCharsets.UTF_8));

            System.out.println("[codeperf] 扫描类数: " + result.getClassesScanned()
                    + "，静态发现: " + result.getFindings().size() + " 条");
            System.out.println("[codeperf] 静态结果已写入: " + output);

            if (report != null && !report.trim().isEmpty()) {
                String html = HtmlReport.generateStatic(result);
                Files.write(Paths.get(report), html.getBytes(StandardCharsets.UTF_8));
                System.out.println("[codeperf] 静态 HTML 报告: " + report);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("[codeperf] 静态扫描失败: " + e.getMessage());
            e.printStackTrace();
            return 2;
        }
    }
}
