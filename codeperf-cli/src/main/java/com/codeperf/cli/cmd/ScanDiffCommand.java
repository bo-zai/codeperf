package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.staticanalysis.ClasspathResolver;
import com.codeperf.analysis.staticanalysis.StaticResult;
import com.codeperf.analysis.staticanalysis.StaticScanner;
import com.codeperf.cli.server.CodePerfServerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Parameters(commandDescription = "Static scan for Git diff scope")
public class ScanDiffCommand {

    @Parameter(names = "--base", description = "Base ref for diff")
    private String base = "origin/main";

    @Parameter(names = "--head", description = "Head ref for diff")
    private String head = "HEAD";

    @Parameter(names = "--target-package", description = "Application package prefix to scan", required = true)
    private String targetPackage;

    @Parameter(names = "--classes-dir", description = "Compiled classes dir")
    private String classesDir;

    @Parameter(names = "--source-root", description = "Source root(s), repeat or comma-separate")
    private List<String> sourceRoots = new ArrayList<>();

    @Parameter(names = "--output", description = "Static result JSON output")
    private String output = "perf-static.json";

    @Parameter(names = "--server", description = "CodePerf Server URL")
    private String server;

    @Parameter(names = "--task-id", description = "Analysis task ID")
    private String taskId;

    @Parameter(names = "--upload", description = "Upload result to server")
    private boolean upload;

    public int execute() {
        if (upload) {
            if (CommandSupport.isBlank(server) || CommandSupport.isBlank(taskId)) {
                System.err.println("[codeperf] --upload requires --server and --task-id");
                return 2;
            }
        }
        File dir = ClasspathResolver.resolve(classesDir);
        if (dir == null) {
            System.err.println("[codeperf] 未找到编译产物目录。请用 --classes-dir 指定。");
            return 2;
        }
        try {
            StaticResult result = new StaticScanner().scan(dir, targetPackage, effectiveSourceRoots());
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Files.write(Paths.get(output), mapper.writeValueAsBytes(result));
            System.out.println("[codeperf] scan-diff base=" + base + ", head=" + head
                    + ", findings=" + result.getFindings().size());
            if (upload) {
                new CodePerfServerClient(server).uploadStaticResult(taskId, mapper.writeValueAsString(result));
                System.out.println("[codeperf] 静态结果已上传: task=" + taskId);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("[codeperf] scan-diff failed: " + e.getMessage());
            return 2;
        }
    }

    private List<String> effectiveSourceRoots() {
        List<String> roots = new ArrayList<>();
        if (sourceRoots != null) {
            for (String value : sourceRoots) {
                if (value != null) {
                    roots.addAll(Arrays.asList(value.split(",")));
                }
            }
        }
        List<String> cleaned = new ArrayList<>();
        for (String root : roots) {
            String r = root.trim();
            if (!r.isEmpty()) {
                cleaned.add(r);
            }
        }
        if (cleaned.isEmpty()) {
            cleaned.add("src/main/java");
            cleaned.add("src/test/java");
        }
        return cleaned;
    }
}
