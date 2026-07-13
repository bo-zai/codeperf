package com.codeperf.cli.report;

import com.codeperf.analysis.source.SourceScanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
