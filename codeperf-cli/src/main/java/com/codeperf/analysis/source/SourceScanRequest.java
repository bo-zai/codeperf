package com.codeperf.analysis.source;

import com.codeperf.cli.config.StaticScanConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
@AllArgsConstructor
public class SourceScanRequest {
    private final Path rootDirectory;
    private final List<Path> sourceFiles;
    private final StaticScanConfig config;
}
