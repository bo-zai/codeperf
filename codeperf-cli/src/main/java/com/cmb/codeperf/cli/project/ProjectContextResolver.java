package com.cmb.codeperf.cli.project;

import com.cmb.codeperf.cli.config.CodePerfCliConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 项目上下文解析器：从工作目录向上查找 Git 根目录和配置文件。
 * <p>
 * 解析流程：
 * <ol>
 *   <li>从 startDirectory 向上查找 .git 目录，确定仓库根目录</li>
 *   <li>在根目录查找 .codeperf.yml 配置文件</li>
 *   <li>加载配置并构造 ProjectContext</li>
 * </ol>
 * <p>
 * 失败场景：
 * <ul>
 *   <li>不在 Git 仓库内（未找到 .git）</li>
 *   <li>未执行 codeperf init（未找到 .codeperf.yml）</li>
 * </ul>
 */
public class ProjectContextResolver {

    public ProjectContext resolve(Path startDirectory) throws IOException {
        Path root = findGitRoot(startDirectory.toAbsolutePath().normalize());
        Path config = root.resolve(".codeperf.yml");
        if (!Files.isRegularFile(config)) {
            throw new IOException("未找到 .codeperf.yml，请先执行 codeperf init");
        }
        return new ProjectContext(root, config, CodePerfCliConfig.load(config));
    }

    private Path findGitRoot(Path start) throws IOException {
        Path current = Files.isDirectory(start) ? start : start.getParent();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IOException("未找到 Git 根目录，请在 Git 仓库内执行");
    }
}

