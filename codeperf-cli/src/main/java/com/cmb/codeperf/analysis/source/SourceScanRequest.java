package com.cmb.codeperf.analysis.source;

import com.cmb.codeperf.cli.config.StaticScanConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

/**
 * 扫描请求：封装项目根目录、源码文件列表和扫描配置。
 * <p>
 * 使用示例：
 * <pre>
 * SourceScanRequest request = new SourceScanRequest(
 *     projectRoot,
 *     changedFiles,
 *     config.getStaticScan()
 * );
 * SourceScanResult result = new SourceScanner().scan(request);
 * </pre>
 */
@Getter
@AllArgsConstructor
public class SourceScanRequest {
    private final Path rootDirectory;
    private final List<Path> sourceFiles;
    private final StaticScanConfig config;
}

