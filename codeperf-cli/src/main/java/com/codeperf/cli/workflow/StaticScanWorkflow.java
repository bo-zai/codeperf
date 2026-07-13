package com.codeperf.cli.workflow;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.staticanalysis.ClasspathResolver;
import com.codeperf.analysis.staticanalysis.StaticFinding;
import com.codeperf.analysis.staticanalysis.StaticResult;
import com.codeperf.analysis.staticanalysis.StaticScanner;
import com.codeperf.cli.git.GitDiffResolver;
import com.codeperf.cli.git.StaticResultDiffFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 静态扫描编排流程：解析编译产物、执行字节码扫描、按 Git diff 收敛结果并写出 JSON。
 * 命令层只负责参数和退出码，核心流程集中在这里，方便后续接入更多静态规则。
 */
public class StaticScanWorkflow {

    private final StaticScanner scanner;

    public StaticScanWorkflow() {
        this(new StaticScanner());
    }

    public StaticScanWorkflow(StaticScanner scanner) {
        this.scanner = scanner;
    }

    public Result scanDiff(Request request) throws IOException {
        File classesDir = ClasspathResolver.resolve(request.getClassesDir());
        if (classesDir == null) {
            throw new IOException("未找到编译产物目录，请先构建项目或配置 classesDir");
        }
        List<String> changedJavaFiles = GitDiffResolver.changedJavaFiles(
                request.getWorkingDirectory(), request.getBaseRef(), request.getHeadRef(), request.getDiffMode());
        StaticResult fullResult = scanner.scan(classesDir, request.getTargetPackage(), request.getSourceRoots());
        StaticResult filteredResult = StaticResultDiffFilter.filter(fullResult, changedJavaFiles);
        String json = toJson(filteredResult);
        Files.write(Paths.get(request.getOutput()), json.getBytes(StandardCharsets.UTF_8));
        return new Result(filteredResult, changedJavaFiles, highestRisk(filteredResult), json);
    }

    private String toJson(StaticResult result) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(result);
    }

    private String highestRisk(StaticResult result) {
        int max = 0;
        String risk = "NONE";
        if (result == null || result.getFindings() == null) {
            return risk;
        }
        for (StaticFinding finding : result.getFindings()) {
            Severity severity = finding.getSeverity();
            if (severity != null && severity.getLevel() > max) {
                max = severity.getLevel();
                risk = severity.name();
            }
        }
        return risk;
    }

    @Getter
    @AllArgsConstructor
    public static class Request {
        private final Path workingDirectory;
        private final String baseRef;
        private final String headRef;
        private final String diffMode;
        private final String targetPackage;
        private final String classesDir;
        private final List<String> sourceRoots;
        private final String output;
    }

    @Getter
    @AllArgsConstructor
    public static class Result {
        private final StaticResult staticResult;
        private final List<String> changedJavaFiles;
        private final String riskLevel;
        private final String json;
    }
}
