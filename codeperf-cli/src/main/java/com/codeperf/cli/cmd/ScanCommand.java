package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.source.RiskAttribution;
import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.SourceScanRequest;
import com.codeperf.analysis.source.SourceScanResult;
import com.codeperf.analysis.source.SourceScanner;
import com.codeperf.cli.attribution.GitRiskAttributionEnricher;
import com.codeperf.cli.config.StaticScanConfig;
import com.codeperf.cli.module.SourceModuleResolver;
import com.codeperf.cli.config.UploadReportConfig;
import com.codeperf.cli.git.GitDiffResolver;
import com.codeperf.cli.project.ProjectContext;
import com.codeperf.cli.project.ProjectContextResolver;
import com.codeperf.cli.report.SourceScanHtmlReportWriter;
import com.codeperf.cli.report.SourceScanJsonReportWriter;
import com.codeperf.cli.upload.GitMetadata;
import com.codeperf.cli.upload.GitMetadataResolver;
import com.codeperf.cli.upload.StaticReportUploadRequest;
import com.codeperf.cli.upload.StaticReportUploader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 扫描命令：执行 Java 源码静态分析并生成报告。
 * <p>
 * 核心流程：
 * <ol>
 *   <li>解析项目上下文（配置文件、Git 根目录）</li>
 *   <li>获取变更文件列表（通过 git diff 或全量扫描）</li>
 *   <li>执行 AST 分析，检测循环 I/O、N+1 等性能风险</li>
 *   <li>对发现执行风险归因（NEW/MODIFIED/HISTORICAL）</li>
 *   <li>生成 JSON/HTML 报告，判定门禁结果</li>
 *   <li>可选上传报告到服务端</li>
 * </ol>
 * <p>
 * 设计决策：
 * <ul>
 *   <li>变更模式：默认只扫描变更文件，通过 git diff 获取；{@code --all} 参数可全量扫描</li>
 *   <li>门禁判定：区分新增风险和历史风险，仅新增风险阻断构建</li>
 *   <li>报告格式：JSON 用于机器读取（CI 集成），HTML 用于人工审查</li>
 * </ul>
 * 使用示例：
 * <ul>
 *   <li>{@code codeperf scan} — 扫描变更文件</li>
 *   <li>{@code codeperf scan --all} — 全量扫描</li>
 *   <li>{@code codeperf scan --upload} — 扫描并上传报告</li>
 * </ul>
 */
@Parameters(commandDescription = "Local AST scan for changed Java source files")
public class ScanCommand {

    private static final int MAX_CONSOLE_FINDINGS = 5;

    @Parameter(names = "--all", description = "Scan all configured source files")
    private boolean scanAll;

    @Parameter(names = "--output", description = "Source scan JSON output")
    private String output;

    @Parameter(names = "--upload", description = "Upload source scan report to CodePerf server")
    private boolean upload;

    private Path workingDirectory;

    public int execute() {
        try {
            // 解析项目上下文：必须在 Git 仓库内执行，因为扫描依赖 git diff 获取变更文件
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            StaticScanConfig config = context.getConfig().getStaticScan();
            SourceModuleResolver moduleResolver = new SourceModuleResolver(context.getConfig().getModules());

            // 获取变更文件：变更模式使用 git diff，全量模式遍历 sourceRoots 目录
            List<Path> files = scanAll
                    ? resolveAllSourceFiles(context, config)
                    : GitDiffResolver.changedJavaFilePaths(context.getRootDirectory(),
                    config.getBaseRef(), config.getHeadRef(), config.getMode());

            // 执行源码扫描：先构建类索引再执行规则，支持跨方法调用链追踪
            SourceScanResult result = new SourceScanner().scan(new SourceScanRequest(
                    context.getRootDirectory(), filterConfiguredSourceFiles(context, config, files), config));

            // 风险归因：仅变更模式执行，通过 git blame 区分新增/历史风险，门禁仅阻断新增风险
            if (!scanAll) {
                result = new GitRiskAttributionEnricher().enrich(
                        result,
                        context.getRootDirectory(),
                        config.getBaseRef(),
                        config.getHeadRef(),
                        config.getMode());
            }

            // 生成报告：JSON 用于机器读取和上传，HTML 用于人工审查
            Path reportPath = context.resolvePath(resolveOutputPath(context));
            boolean uploadRequested = shouldUpload(context);
            boolean localReportEnabled = context.getConfig().getReport().getLocal().isEnabled();
            boolean jsonReportWritten = localReportEnabled || uploadRequested;
            if (jsonReportWritten) {
                new SourceScanJsonReportWriter().write(reportPath, result);
            }

            // 门禁判定：根据严重度和归因决定是否阻断构建，历史风险不阻断
            StaticGateDecision gateDecision = new StaticGateEvaluator()
                    .evaluate(result, config.getFailOn(), !scanAll);
            Path htmlReportPath = resolveHtmlReportPath(reportPath);
            if (localReportEnabled) {
                new SourceScanHtmlReportWriter().write(htmlReportPath, result, context.getRootDirectory(), context.getConfig().getModules());
            }
            printSummary(result, gateDecision, moduleResolver);
            if (jsonReportWritten) {
                System.out.println("[codeperf] jsonReport=" + displayPath(context, reportPath));
            }
            if (localReportEnabled) {
                System.out.println("[codeperf] htmlReport=" + displayPath(context, htmlReportPath));
                System.out.println("[codeperf] htmlReportUrl=" + htmlReportPath.toAbsolutePath().normalize().toUri());
            }
            printBlockingFindings(result, config.getFailOn(), !scanAll, moduleResolver);
            if (gateDecision.getHistorical() > 0) {
                System.out.println("[codeperf] 历史风险数量=" + gateDecision.getHistorical() + "，请查看 HTML 报告详情。");
            }
            if (uploadRequested) {
                uploadReport(context, reportPath);
            }
            return gateDecision.isFailed() ? 1 : 0;
        } catch (Exception e) {
            System.err.println("[codeperf] scan 失败: " + e.getMessage());
            return 2;
        }
    }

    private void printSummary(SourceScanResult result, StaticGateDecision gateDecision, SourceModuleResolver moduleResolver) {
        System.out.println("[codeperf] 扫描文件=" + result.getFilesScanned()
                + "，风险总数=" + result.getFindings().size()
                + "，阻断风险=" + gateDecision.getBlocking()
                + "，结果=" + (gateDecision.isFailed() ? "失败" : "通过")
                + "，解析错误=" + result.getParseErrors().size());
        System.out.println(gateDecision.summary());
        printModuleSummary(result, moduleResolver);
    }

    private void printModuleSummary(SourceScanResult result, SourceModuleResolver moduleResolver) {
        List<String> modules = new ArrayList<>();
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

    private void printBlockingFindings(SourceScanResult result, String failOn, boolean attributionAware,
            SourceModuleResolver moduleResolver) {
        int printed = 0;
        int totalBlocking = 0;
        for (SourceFinding finding : result.getFindings()) {
            if (!isBlockingFinding(finding, failOn, attributionAware)) {
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

    private boolean isBlockingFinding(SourceFinding finding, String failOn, boolean attributionAware) {
        if (!CommandSupport.shouldFail(finding.getSeverity().name(), failOn)) {
            return false;
        }
        if (!attributionAware) {
            return true;
        }
        RiskAttribution.RiskScope riskScope = finding.getAttribution().getRiskScope();
        return RiskAttribution.RiskScope.NEW.equals(riskScope)
                || RiskAttribution.RiskScope.MODIFIED.equals(riskScope);
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

    private String valueOrUnknown(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        return value;
    }

    private Path resolveHtmlReportPath(Path reportPath) {
        Path fileName = reportPath.getFileName();
        if (fileName == null) {
            return reportPath.resolveSibling("source-report.html");
        }
        String name = fileName.toString();
        String htmlName;
        if (name.endsWith(".json")) {
            htmlName = name.substring(0, name.length() - ".json".length()) + ".html";
        } else {
            htmlName = name + ".html";
        }
        Path parent = reportPath.getParent();
        return parent == null ? Paths.get(htmlName) : parent.resolve(htmlName);
    }

    private String displayPath(ProjectContext context, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path root = context.getRootDirectory().toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return normalized.toString();
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    void setOutputForTest(String output) {
        this.output = output;
    }

    private List<Path> resolveAllSourceFiles(ProjectContext context, StaticScanConfig config) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String sourceRoot : SourceModuleResolver.effectiveSourceRoots(config, context.getConfig().getModules())) {
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
        for (String sourceRoot : SourceModuleResolver.effectiveSourceRoots(config, context.getConfig().getModules())) {
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
                metadata.getRemoteUrl(),
                metadata.getAuthorName(),
                metadata.getAuthorEmail(),
                metadata.getAuthorTime(),
                metadata.getCommitterName(),
                metadata.getCommitterEmail(),
                metadata.getCommitMessage(),
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
