package com.cmb.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.cmb.codeperf.analysis.source.RiskAttribution;
import com.cmb.codeperf.analysis.source.SourceFinding;
import com.cmb.codeperf.analysis.source.SourceScanRequest;
import com.cmb.codeperf.analysis.source.SourceScanResult;
import com.cmb.codeperf.analysis.source.SourceScanner;
import com.cmb.codeperf.cli.config.StaticScanConfig;
import com.cmb.codeperf.cli.git.GitLocalChangeResolver;
import com.cmb.codeperf.cli.git.GitLocalChangeSet;
import com.cmb.codeperf.cli.module.SourceModuleResolver;
import com.cmb.codeperf.cli.project.ProjectContext;
import com.cmb.codeperf.cli.project.ProjectContextResolver;
import com.cmb.codeperf.cli.report.SourceScanHtmlReportWriter;
import com.cmb.codeperf.cli.report.SourceScanJsonReportWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地预检命令：扫描开发者尚未进入 push 门禁的本地 Java 变更。
 * <p>
 * 与 {@code scan} 的区别：
 * <ul>
 *   <li>{@code scan} 面向 Git hook/pre-push，只关注推送范围</li>
 *   <li>{@code pre-scan} 面向编码阶段，覆盖已提交未推送、暂存、未暂存、未跟踪文件</li>
 * </ul>
 */
@Parameters(commandDescription = "Local pre-check for worktree, staged and untracked Java source files")
public class PreScanCommand {

    private static final int MAX_CONSOLE_FINDINGS = 5;

    @Parameter(names = "--output", description = "Source pre-scan JSON output")
    private String output;

    private Path workingDirectory;

    /**
     * 执行本地预检。
     *
     * @return 退出码，存在阻断风险时返回 1
     */
    public int execute() {
        try {
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            StaticScanConfig config = context.getConfig().getStaticScan();
            SourceModuleResolver moduleResolver = new SourceModuleResolver(context.getConfig().getModules());

            GitLocalChangeSet changeSet = new GitLocalChangeResolver().resolve(context.getRootDirectory());
            List<Path> files = filterConfiguredSourceFiles(context, config, changeSet.getSourceFiles());
            SourceScanResult rawResult = new SourceScanner().scan(new SourceScanRequest(
                    context.getRootDirectory(), files, config));
            SourceScanResult result = markLocalUncommitted(rawResult, context.getRootDirectory());

            Path reportPath = context.resolvePath(resolveOutputPath(context));
            boolean localReportEnabled = context.getConfig().getReport().getLocal().isEnabled();
            if (localReportEnabled) {
                new SourceScanJsonReportWriter().write(reportPath, result);
                new SourceScanHtmlReportWriter().write(
                        resolveHtmlReportPath(reportPath),
                        result,
                        context.getRootDirectory(),
                        context.getConfig().getModules());
            }

            StaticGateDecision gateDecision = new StaticGateEvaluator().evaluate(result, config.getFailOn(), true);
            printSummary(result, gateDecision, changeSet, moduleResolver);
            if (localReportEnabled) {
                Path htmlReportPath = resolveHtmlReportPath(reportPath);
                System.out.println("[codeperf] jsonReport=" + displayPath(context, reportPath));
                System.out.println("[codeperf] htmlReport=" + displayPath(context, htmlReportPath));
                System.out.println("[codeperf] htmlReportUrl=" + htmlReportPath.toAbsolutePath().normalize().toUri());
            }
            printBlockingFindings(result, config.getFailOn(), moduleResolver);
            return gateDecision.isFailed() ? 1 : 0;
        } catch (Exception e) {
            System.err.println("[codeperf] pre-scan 失败: " + e.getMessage());
            return 2;
        }
    }

    private SourceScanResult markLocalUncommitted(SourceScanResult result, Path rootDirectory) {
        LocalGitUser user = resolveLocalGitUser(rootDirectory);
        List<SourceFinding> findings = new ArrayList<SourceFinding>();
        for (SourceFinding finding : result.getFindings()) {
            findings.add(finding.withAttribution(new RiskAttribution(
                    RiskAttribution.RiskScope.LOCAL_UNCOMMITTED,
                    true,
                    RiskAttribution.AttributionConfidence.MEDIUM,
                    user.name,
                    user.email,
                    "",
                    "",
                    "")));
        }
        return new SourceScanResult(result.getFilesScanned(), result.getScannedSourceFiles(), findings,
                result.getParseErrors());
    }

    private LocalGitUser resolveLocalGitUser(Path rootDirectory) {
        return new LocalGitUser(
                runGitFirstLine(rootDirectory, "config", "user.name"),
                runGitFirstLine(rootDirectory, "config", "user.email"));
    }

    private String runGitFirstLine(Path workingDirectory, String... args) {
        List<String> command = new ArrayList<String>();
        command.add("git");
        for (String arg : args) {
            command.add(arg);
        }
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                line = reader.readLine();
            }
            int exitCode = waitFor(process);
            if (exitCode != 0 || line == null) {
                return "";
            }
            return line.trim();
        } catch (IOException e) {
            return "";
        }
    }

    private int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 git 命令执行被中断", e);
        }
    }

    private void printSummary(SourceScanResult result, StaticGateDecision gateDecision,
                              GitLocalChangeSet changeSet, SourceModuleResolver moduleResolver) {
        System.out.println("[codeperf] 命令=pre-scan，用途=本地编码预检，范围=worktree + staged + untracked");
        System.out.println("[codeperf] 变更来源=已提交未推送:" + changeSet.getCommittedNotPushedCount()
                + "，暂存:" + changeSet.getStagedCount()
                + "，未暂存:" + changeSet.getUnstagedCount()
                + "，未跟踪:" + changeSet.getUntrackedCount());
        System.out.println("[codeperf] 扫描文件=" + result.getFilesScanned()
                + "，风险总数=" + result.getFindings().size()
                + "，阻断风险=" + gateDecision.getBlocking()
                + "，结果=" + (gateDecision.isFailed() ? "失败" : "通过")
                + "，解析错误=" + result.getParseErrors().size());
        System.out.println(gateDecision.summary());
        printModuleSummary(result, moduleResolver);
    }

    private void printModuleSummary(SourceScanResult result, SourceModuleResolver moduleResolver) {
        List<String> modules = new ArrayList<String>();
        for (SourceFinding finding : result.getFindings()) {
            String moduleName = moduleResolver.resolveModuleName(finding.getSourceFile());
            if (!modules.contains(moduleName)) {
                modules.add(moduleName);
            }
        }
        for (String module : modules) {
            int moduleFindings = 0;
            for (SourceFinding finding : result.getFindings()) {
                if (module.equals(moduleResolver.resolveModuleName(finding.getSourceFile()))) {
                    moduleFindings++;
                }
            }
            System.out.println("[codeperf] 模块 " + module + "：风险=" + moduleFindings);
        }
    }

    private void printBlockingFindings(SourceScanResult result, String failOn, SourceModuleResolver moduleResolver) {
        int printed = 0;
        int totalBlocking = 0;
        for (SourceFinding finding : result.getFindings()) {
            if (!CommandSupport.shouldFail(finding.getSeverity().name(), failOn)) {
                continue;
            }
            totalBlocking++;
            if (printed < MAX_CONSOLE_FINDINGS) {
                System.out.println(formatBlockingFinding(finding, moduleResolver.resolveModuleName(finding.getSourceFile())));
                printed++;
            }
        }
        if (totalBlocking > MAX_CONSOLE_FINDINGS) {
            System.out.println("[codeperf] 阻断风险已截断，已显示=" + MAX_CONSOLE_FINDINGS
                    + "，总数=" + totalBlocking + "，请查看 HTML 报告详情。");
        }
    }

    private String formatBlockingFinding(SourceFinding finding, String moduleName) {
        RiskAttribution attribution = finding.getAttribution();
        return "[codeperf] 阻断风险 " + finding.getRuleId()
                + " " + finding.getSeverity().name()
                + " " + finding.getConfidence().name()
                + " 模块=" + valueOrUnknown(moduleName)
                + " 位置=" + finding.getSourceFile() + ":" + finding.getLineNumber()
                + " 方法=" + valueOrUnknown(finding.getLoopMethodName())
                + " I/O=" + valueOrUnknown(finding.getIoType())
                + " 归因=" + attribution.getRiskScope().name()
                + " 提交人=" + valueOrUnknown(attribution.getIntroducedByName())
                + " 邮箱=" + valueOrUnknown(attribution.getIntroducedByEmail());
    }

    private List<Path> filterConfiguredSourceFiles(ProjectContext context, StaticScanConfig config, List<Path> files) {
        List<Path> roots = new ArrayList<Path>();
        for (String sourceRoot : SourceModuleResolver.effectiveSourceRoots(config, context.getConfig().getModules())) {
            roots.add(context.resolvePath(sourceRoot));
        }
        List<Path> filtered = new ArrayList<Path>();
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

    private Path resolveHtmlReportPath(Path reportPath) {
        Path fileName = reportPath.getFileName();
        if (fileName == null) {
            return reportPath.resolveSibling("source-report.html");
        }
        String name = fileName.toString();
        String htmlName = name.endsWith(".json")
                ? name.substring(0, name.length() - ".json".length()) + ".html"
                : name + ".html";
        Path parent = reportPath.getParent();
        return parent == null ? Paths.get(htmlName) : parent.resolve(htmlName);
    }

    private String resolveOutputPath(ProjectContext context) {
        if (output != null && !output.trim().isEmpty()) {
            return output;
        }
        return context.getConfig().getReport().getLocal().getPath();
    }

    private String displayPath(ProjectContext context, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path root = context.getRootDirectory().toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return normalized.toString();
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        return value;
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    private static class LocalGitUser {
        private final String name;
        private final String email;

        private LocalGitUser(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
