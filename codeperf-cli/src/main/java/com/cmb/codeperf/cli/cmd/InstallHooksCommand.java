package com.cmb.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;
import com.cmb.codeperf.cli.project.ProjectContext;
import com.cmb.codeperf.cli.project.ProjectContextResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 安装 Git 钩子命令：在 {@code .git/hooks/pre-push} 中写入 {@code codeperf scan} 调用。
 * <p>
 * 设计决策：
 * <ul>
 *   <li>幂等性：若钩子文件已存在则不覆盖，仅提示用户手工合并内容</li>
 *   <li>选择 pre-push 而非 pre-commit：避免影响提交体验，在推送前执行门禁检查</li>
 * </ul>
 * 使用示例：{@code codeperf install-hooks}
 */
@Parameters(commandDescription = "Install CodePerf pre-push hook")
public class InstallHooksCommand {

    private Path workingDirectory;

    /**
     * 执行钩子安装命令。
     *
     * @return 退出码：0 安装成功或已存在，2 执行异常
     */
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

