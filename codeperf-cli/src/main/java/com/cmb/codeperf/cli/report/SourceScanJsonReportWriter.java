package com.cmb.codeperf.cli.report;

import com.cmb.codeperf.analysis.source.SourceScanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON 报告生成器：将扫描结果序列化为 JSON 文件，用于机器读取和后续处理。
 * <p>
 * 用途：
 * <ul>
 *   <li>CI 集成：解析 JSON 判断门禁结果</li>
 *   <li>服务端上传：JSON 作为上传请求体</li>
 *   <li>历史对比：存储 JSON 用于趋势分析</li>
 * </ul>
 */
public class SourceScanJsonReportWriter {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(Path output, SourceScanResult result) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        String json = objectMapper.writeValueAsString(result);
        Files.write(output, json.getBytes(StandardCharsets.UTF_8));
    }
}

