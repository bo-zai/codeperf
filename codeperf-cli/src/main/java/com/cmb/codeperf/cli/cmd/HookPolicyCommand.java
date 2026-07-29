package com.cmb.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.cmb.codeperf.cli.project.ProjectContext;
import com.cmb.codeperf.cli.project.ProjectContextResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Hook 策略查询命令。
 * Git hook 脚本只适合做流程编排，策略仍由 Java 读取 .codeperf.yml，避免 shell 解析 YAML。
 */
@Parameters(commandDescription = "Resolve CodePerf git hook policy")
public class HookPolicyCommand {

    @Parameter(description = "hook name")
    private List<String> hooks = new ArrayList<>();

    private Path workingDirectory;

    /**
     * 输出指定 hook 是否应该阻断。
     *
     * @return 退出码，读取失败时仍返回 0 并输出默认 false
     */
    public int execute() {
        try {
            String hookName = hooks.isEmpty() ? "" : hooks.get(0);
            Path cwd = workingDirectory == null ? Paths.get(".") : workingDirectory;
            ProjectContext context = new ProjectContextResolver().resolve(cwd);
            boolean block = "pre-push".equals(hookName)
                    && context.getConfig().getGitHooks().getPrePush().isBlockOnFailure();
            System.out.println(block);
            return 0;
        } catch (Exception e) {
            // hook 策略读取失败时不能影响开发推送，扫描命令本身会输出具体失败原因。
            System.out.println(false);
            return 0;
        }
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    void setHooksForTest(String hookName) {
        this.hooks.clear();
        this.hooks.add(hookName);
    }
}
