package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;
import com.codeperf.cli.project.ProjectContext;
import com.codeperf.cli.project.ProjectContextResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandDescription = "Install CodePerf pre-push hook")
public class InstallHooksCommand {

    private Path workingDirectory;

    public int execute() {
        try {
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            Path hook = context.getRootDirectory().resolve(".git/hooks/pre-push");
            Files.createDirectories(hook.getParent());
            if (Files.exists(hook)) {
                System.out.println("[codeperf] pre-push hook 已存在，未覆盖: " + hook);
                System.out.println("[codeperf] 如需启用 CodePerf，请手工合并以下内容:");
                System.out.print(hookContent());
                return 0;
            }
            Files.write(hook, hookContent().getBytes(StandardCharsets.UTF_8));
            hook.toFile().setExecutable(true);
            System.out.println("[codeperf] 已安装 pre-push hook");
            return 0;
        } catch (Exception e) {
            System.err.println("[codeperf] install-hooks 失败: " + e.getMessage());
            return 2;
        }
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    private String hookContent() {
        return "#!/usr/bin/env sh\n"
                + "codeperf scan\n";
    }
}
