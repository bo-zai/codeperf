package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.cli.config.CodePerfCliConfig;
import com.codeperf.cli.git.GitMetadataResolver;
import com.codeperf.cli.server.CodePerfServerClient;
import com.codeperf.cli.server.GateResult;
import com.codeperf.cli.workflow.StaticScanWorkflow;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CI 一站式入口。
 * 自动创建任务、执行 diff-aware 静态扫描、上传结果并查询门禁，避免流水线手工拼接 taskId。
 */
@Parameters(commandDescription = "Run CodePerf CI flow from .codeperf.yml")
public class CiRunCommand {

    @Parameter(names = "--config", description = "CodePerf config file")
    private String configPath = ".codeperf.yml";

    @Parameter(names = "--output", description = "Static result JSON output")
    private String output = "perf-static.json";

    public int execute() {
        try {
            CodePerfCliConfig config = CodePerfCliConfig.load(Paths.get(configPath));
            String validationError = validate(config);
            if (validationError != null) {
                System.err.println("[codeperf] 配置不完整: " + validationError);
                return 2;
            }
            Path workingDirectory = Paths.get(".");
            String commit = GitMetadataResolver.resolveCommit(workingDirectory, System.getenv());
            String branch = GitMetadataResolver.resolveBranch(workingDirectory, System.getenv());
            CodePerfServerClient client = new CodePerfServerClient(config.getServerUrl());
            String taskId = client.createTask(config.getProject(), commit, branch, config.getEnv());
            StaticScanWorkflow.Result scan = new StaticScanWorkflow().scanDiff(new StaticScanWorkflow.Request(
                    workingDirectory,
                    config.getBaseRef(),
                    config.getHeadRef(),
                    config.getDiffMode(),
                    config.getTargetPackage(),
                    config.getClassesDir(),
                    config.sourceRootsOrDefault(),
                    output));
            client.uploadStaticResult(taskId, scan.getJson());
            GateResult gate = client.getGate(taskId);
            printSummary(taskId, commit, branch, scan, gate);
            return CommandSupport.shouldFail(gate.getRiskLevel(), config.getFailOn()) ? 1 : 0;
        } catch (Exception e) {
            System.err.println("[codeperf] ci-run 失败: " + e.getMessage());
            return 2;
        }
    }

    private String validate(CodePerfCliConfig config) {
        if (CommandSupport.isBlank(config.getServerUrl())) {
            return "serverUrl 不能为空";
        }
        if (CommandSupport.isBlank(config.getProject())) {
            return "project 不能为空";
        }
        if (CommandSupport.isBlank(config.getTargetPackage())) {
            return "targetPackage 不能为空";
        }
        if (CommandSupport.isBlank(config.getClassesDir())) {
            return "classesDir 不能为空";
        }
        return null;
    }

    private void printSummary(String taskId, String commit, String branch,
                              StaticScanWorkflow.Result scan, GateResult gate) {
        System.out.println("[codeperf] task=" + taskId
                + ", commit=" + commit
                + ", branch=" + branch
                + ", changedJavaFiles=" + scan.getChangedJavaFiles().size()
                + ", staticFindings=" + scan.getStaticResult().getFindings().size()
                + ", gateRisk=" + gate.getRiskLevel());
    }
}
