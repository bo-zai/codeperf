package com.codeperf.cli.project;

import com.codeperf.cli.config.CodePerfCliConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

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
