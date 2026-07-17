package com.codeperf.cli.project;

import com.codeperf.cli.config.CodePerfCliConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

/**
 * 项目上下文：封装 Git 根目录、配置文件路径和已加载的配置对象。
 * <p>
 * 设计意图：避免在扫描流程中频繁传递多个参数，将项目相关信息聚合为单一对象。
 * <p>
 * 使用示例：
 * <pre>
 * ProjectContext context = new ProjectContextResolver().resolve(Paths.get("."));
 * Path sourceRoot = context.resolvePath("src/main/java");
 * String failOn = context.getConfig().getFailOn();
 * </pre>
 */
@Getter
@AllArgsConstructor
public class ProjectContext {
    private final Path rootDirectory;
    private final Path configPath;
    private final CodePerfCliConfig config;

    public Path resolvePath(String path) {
        return rootDirectory.resolve(path).toAbsolutePath().normalize();
    }
}
