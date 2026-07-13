package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.SourceScanRequest;
import com.codeperf.analysis.source.SourceScanResult;
import com.codeperf.analysis.source.SourceScanner;
import com.codeperf.cli.config.StaticScanConfig;
import com.codeperf.cli.git.GitDiffResolver;
import com.codeperf.cli.project.ProjectContext;
import com.codeperf.cli.project.ProjectContextResolver;
import com.codeperf.cli.report.SourceScanJsonReportWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Parameters(commandDescription = "Local AST scan for changed Java source files")
public class ScanCommand {

    @Parameter(names = "--all", description = "Scan all configured source files")
    private boolean scanAll;

    @Parameter(names = "--output", description = "Source scan JSON output")
    private String output = ".codeperf/report/source-report.json";

    private Path workingDirectory;

    public int execute() {
        try {
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            StaticScanConfig config = context.getConfig().getStaticScan();
            List<Path> files = scanAll
                    ? resolveAllSourceFiles(context, config)
                    : GitDiffResolver.changedJavaFilePaths(context.getRootDirectory(),
                    config.getBaseRef(), config.getHeadRef(), "range");
            SourceScanResult result = new SourceScanner().scan(new SourceScanRequest(
                    context.getRootDirectory(), filterConfiguredSourceFiles(context, config, files), config));
            new SourceScanJsonReportWriter().write(context.resolvePath(output), result);
            System.out.println("[codeperf] sourceFiles=" + result.getFilesScanned()
                    + ", findings=" + result.getFindings().size()
                    + ", parseErrors=" + result.getParseErrors().size());
            return hasFailure(result, config.getFailOn()) ? 1 : 0;
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

    private boolean hasFailure(SourceScanResult result, String failOn) {
        for (SourceFinding finding : result.getFindings()) {
            if (CommandSupport.shouldFail(finding.getSeverity().name(), failOn)) {
                return true;
            }
        }
        return false;
    }
}
