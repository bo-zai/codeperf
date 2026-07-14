package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.source.SourceScanRequest;
import com.codeperf.analysis.source.SourceScanResult;
import com.codeperf.analysis.source.SourceScanner;
import com.codeperf.cli.attribution.GitRiskAttributionEnricher;
import com.codeperf.cli.config.StaticScanConfig;
import com.codeperf.cli.config.UploadReportConfig;
import com.codeperf.cli.git.GitDiffResolver;
import com.codeperf.cli.project.ProjectContext;
import com.codeperf.cli.project.ProjectContextResolver;
import com.codeperf.cli.report.SourceScanJsonReportWriter;
import com.codeperf.cli.upload.GitMetadata;
import com.codeperf.cli.upload.GitMetadataResolver;
import com.codeperf.cli.upload.StaticReportUploadRequest;
import com.codeperf.cli.upload.StaticReportUploader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Parameters(commandDescription = "Local AST scan for changed Java source files")
public class ScanCommand {

    @Parameter(names = "--all", description = "Scan all configured source files")
    private boolean scanAll;

    @Parameter(names = "--output", description = "Source scan JSON output")
    private String output;

    @Parameter(names = "--upload", description = "Upload source scan report to CodePerf server")
    private boolean upload;

    private Path workingDirectory;

    public int execute() {
        try {
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            StaticScanConfig config = context.getConfig().getStaticScan();
            List<Path> files = scanAll
                    ? resolveAllSourceFiles(context, config)
                    : GitDiffResolver.changedJavaFilePaths(context.getRootDirectory(),
                    config.getBaseRef(), config.getHeadRef(), config.getMode());
            SourceScanResult result = new SourceScanner().scan(new SourceScanRequest(
                    context.getRootDirectory(), filterConfiguredSourceFiles(context, config, files), config));
            if (!scanAll) {
                result = new GitRiskAttributionEnricher().enrich(
                        result,
                        context.getRootDirectory(),
                        config.getBaseRef(),
                        config.getHeadRef(),
                        config.getMode());
            }
            Path reportPath = context.resolvePath(resolveOutputPath(context));
            boolean uploadRequested = shouldUpload(context);
            if (context.getConfig().getReport().getLocal().isEnabled() || uploadRequested) {
                new SourceScanJsonReportWriter().write(reportPath, result);
            }
            System.out.println("[codeperf] sourceFiles=" + result.getFilesScanned()
                    + ", findings=" + result.getFindings().size()
                    + ", parseErrors=" + result.getParseErrors().size());
            StaticGateDecision gateDecision = new StaticGateEvaluator()
                    .evaluate(result, config.getFailOn(), !scanAll);
            System.out.println(gateDecision.summary());
            if (uploadRequested) {
                uploadReport(context, reportPath);
            }
            return gateDecision.isFailed() ? 1 : 0;
        } catch (Exception e) {
            System.err.println("[codeperf] scan 失败: " + e.getMessage());
            return 2;
        }
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    void setOutputForTest(String output) {
        this.output = output;
    }

    private List<Path> resolveAllSourceFiles(ProjectContext context, StaticScanConfig config) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String sourceRoot : config.getSourceRoots()) {
            Path root = context.resolvePath(sourceRoot);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                files.addAll(walk
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .collect(Collectors.toList()));
            }
        }
        return files;
    }

    private List<Path> filterConfiguredSourceFiles(ProjectContext context, StaticScanConfig config, List<Path> files) {
        List<Path> roots = new ArrayList<>();
        for (String sourceRoot : config.getSourceRoots()) {
            roots.add(context.resolvePath(sourceRoot));
        }
        List<Path> filtered = new ArrayList<>();
        for (Path file : files) {
            Path normalized = file.toAbsolutePath().normalize();
            if (!normalized.toString().endsWith(".java")) {
                continue;
            }
            for (Path root : roots) {
                if (normalized.startsWith(root)) {
                    filtered.add(normalized);
                    break;
                }
            }
        }
        return filtered;
    }

    private String resolveOutputPath(ProjectContext context) {
        if (output != null && !output.trim().isEmpty()) {
            return output;
        }
        return context.getConfig().getReport().getLocal().getPath();
    }

    private boolean shouldUpload(ProjectContext context) {
        return upload || context.getConfig().getReport().getUpload().isEnabled();
    }

    private void uploadReport(ProjectContext context, Path reportPath) throws Exception {
        UploadReportConfig uploadConfig = context.getConfig().getReport().getUpload();
        String serverUrl = uploadConfig.getServerUrl();
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalStateException("未配置 report.upload.serverUrl");
        }
        if (!Files.isRegularFile(reportPath)) {
            throw new IllegalStateException("未找到本地扫描报告: " + reportPath);
        }
        GitMetadata metadata = new GitMetadataResolver().resolve(context.getRootDirectory());
        StaticReportUploadRequest request = new StaticReportUploadRequest(
                context.getConfig().getProject(),
                context.getConfig().getEnv(),
                metadata.getCommit(),
                metadata.getBranch(),
                new String(Files.readAllBytes(reportPath), StandardCharsets.UTF_8));
        String taskId = new StaticReportUploader().upload(trimTrailingSlash(serverUrl), request);
        System.out.println("[codeperf] static report uploaded, taskId=" + taskId);
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
