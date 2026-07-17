package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;
import com.codeperf.cli.project.ProjectContext;
import com.codeperf.cli.project.ProjectContextResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 诊断命令：验证本地环境配置是否正确。
 * <p>
 * 检查项：
 * <ul>
 *   <li>{@code .codeperf.yml} 配置文件是否存在且可解析</li>
 *   <li>{@code sourceRoots} 配置的源码目录是否存在</li>
 * </ul>
 * 使用示例：{@code codeperf doctor}
 */
@Parameters(commandDescription = "Check CodePerf local config")
public class DoctorCommand {

    private Path workingDirectory;

    /**
     * 执行诊断命令。
     *
     * @return 退出码：0 检查通过，2 配置错误或目录不存在
     */
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
