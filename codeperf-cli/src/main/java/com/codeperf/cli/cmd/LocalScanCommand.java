package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.cli.config.CodePerfCliConfig;
import com.codeperf.cli.workflow.StaticScanWorkflow;

import java.nio.file.Paths;

/**
 * 本地 Git hook 入口。
 * 读取 .codeperf.yml 后执行 diff-aware 静态扫描，不创建服务端任务，适合 pre-commit/pre-push 快速拦截。
 */
@Parameters(commandDescription = "Run local Git diff static scan from .codeperf.yml")
public class LocalScanCommand {

    @Parameter(names = "--config", description = "CodePerf config file")
    private String configPath = ".codeperf.yml";

    @Parameter(names = "--output", description = "Static result JSON output")
    private String output = "perf-static.json";

    @Parameter(names = "--diff-mode", description = "Diff mode: range, staged or worktree")
    private String diffMode;

    public int execute() {
        try {
            CodePerfCliConfig config = CodePerfCliConfig.load(Paths.get(configPath));
            String validationError = validate(config);
            if (validationError != null) {
                System.err.println("[codeperf] 配置不完整: " + validationError);
                return 2;
            }
            StaticScanWorkflow.Result result = new StaticScanWorkflow().scanDiff(new StaticScanWorkflow.Request(
                    Paths.get("."),
                    config.getBaseRef(),
                    config.getHeadRef(),
                    effectiveDiffMode(config),
                    config.getTargetPackage(),
                    config.getClassesDir(),
                    config.sourceRootsOrDefault(),
                    output));
            printSummary(result);
            return CommandSupport.shouldFail(result.getRiskLevel(), config.getFailOn()) ? 1 : 0;
        } catch (Exception e) {
            System.err.println("[codeperf] local-scan 失败: " + e.getMessage());
            return 2;
        }
    }

    private String validate(CodePerfCliConfig config) {
        if (CommandSupport.isBlank(config.getTargetPackage())) {
            return "targetPackage 不能为空";
        }
        if (CommandSupport.isBlank(config.getClassesDir())) {
            return "classesDir 不能为空";
        }
        return null;
    }

    private String effectiveDiffMode(CodePerfCliConfig config) {
        return CommandSupport.isBlank(diffMode) ? config.getDiffMode() : diffMode;
    }

    private void printSummary(StaticScanWorkflow.Result result) {
        System.out.println("[codeperf] Java 变更文件: " + result.getChangedJavaFiles().size()
                + "，静态发现: " + result.getStaticResult().getFindings().size()
                + "，最高风险: " + result.getRiskLevel());
    }
}
