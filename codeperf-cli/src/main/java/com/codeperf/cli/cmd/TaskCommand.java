package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.cli.server.CodePerfServerClient;

import java.io.IOException;

/**
 * 创建分析任务。
 * Git/CI 流程先创建 analysis_task_id，再把静态扫描结果和预发动态证据关联到同一任务。
 */
@Parameters(commandDescription = "Create analysis task on CodePerf Server")
public class TaskCommand {

    @Parameter(names = "--server", description = "CodePerf Server URL", required = true)
    private String server;

    @Parameter(names = "--project", description = "Project name", required = true)
    private String project;

    @Parameter(names = "--commit", description = "Git commit sha", required = true)
    private String commit;

    @Parameter(names = "--branch", description = "Git branch name", required = true)
    private String branch;

    @Parameter(names = "--env", description = "Environment name")
    private String env = "ci";

    public int execute() {
        try {
            String taskId = new CodePerfServerClient(server).createTask(project, commit, branch, env);
            System.out.println(taskId);
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[codeperf] 创建分析任务失败: " + e.getMessage());
            return 2;
        }
    }
}
