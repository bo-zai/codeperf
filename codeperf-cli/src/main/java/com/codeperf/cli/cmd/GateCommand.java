package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.cli.server.CodePerfServerClient;
import com.codeperf.cli.server.GateResult;

import java.io.IOException;

/**
 * 查询门禁结果。
 * CI/pre-push 通过该命令把服务端合并后的风险等级转换为退出码。
 */
@Parameters(commandDescription = "Query CodePerf gate result")
public class GateCommand {

    @Parameter(names = "--server", description = "CodePerf Server URL", required = true)
    private String server;

    @Parameter(names = "--task-id", description = "Analysis task ID", required = true)
    private String taskId;

    @Parameter(names = "--fail-on", description = "Risk threshold: WARN, ERROR or CRITICAL")
    private String failOn = "ERROR";

    public int execute() {
        try {
            GateResult result = new CodePerfServerClient(server).getGate(taskId);
            System.out.println("[codeperf] task=" + result.getAnalysisTaskId()
                    + ", status=" + result.getStatus()
                    + ", risk=" + result.getRiskLevel());
            return CommandSupport.shouldFail(result.getRiskLevel(), failOn) ? 1 : 0;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[codeperf] 查询门禁结果失败: " + e.getMessage());
            return 2;
        }
    }
}
