package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;
import com.codeperf.cli.project.ProjectContext;
import com.codeperf.cli.project.ProjectContextResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandDescription = "Check CodePerf local config")
public class DoctorCommand {

    private Path workingDirectory;

    public int execute() {
        try {
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            for (String sourceRoot : context.getConfig().getStaticScan().getSourceRoots()) {
                Path path = context.resolvePath(sourceRoot);
                if (!Files.isDirectory(path)) {
                    System.err.println("[codeperf] sourceRoot 不存在: " + path);
                    return 2;
                }
            }
            Path agentConfig = context.resolvePath(context.getConfig().getAgent().getConfigPath());
            if (!Files.isRegularFile(agentConfig)) {
                System.err.println("[codeperf] agent 配置不存在: " + agentConfig);
                return 2;
            }
            System.out.println("[codeperf] doctor 检查通过");
            return 0;
        } catch (Exception e) {
            System.err.println("[codeperf] doctor 失败: " + e.getMessage());
            return 2;
        }
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
