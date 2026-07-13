package com.codeperf.cli.project;

import com.codeperf.cli.config.CodePerfCliConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
