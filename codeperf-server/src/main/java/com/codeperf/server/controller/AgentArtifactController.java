package com.codeperf.server.controller;

import com.codeperf.server.config.AgentInstallProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Agent 制品下载接口。
 * 本接口主要用于 local/dev 验证闭环；正式企业环境建议通过 CODEPERF_AGENT_URL 指向制品库或对象存储。
 */
@RestController
@RequestMapping("/api/agent/artifact")
public class AgentArtifactController {

    private static final String AGENT_FILE_NAME = "codeperf-agent.jar";

    private final AgentInstallProperties properties;

    public AgentArtifactController(AgentInstallProperties properties) {
        this.properties = properties;
    }

    /**
     * 下载本地构建出的 agent jar。
     *
     * @return agent jar 二进制内容；未构建时返回 404，避免脚本下载到错误内容
     */
    @GetMapping
    public ResponseEntity<Resource> download() {
        Path artifact = resolveArtifactPath();
        if (artifact == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(artifact.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + AGENT_FILE_NAME + "\"")
                .body(resource);
    }

    private Path resolveArtifactPath() {
        String artifactPath = properties.getLocalArtifactPath();
        if (artifactPath == null || artifactPath.trim().isEmpty()) {
            artifactPath = AgentInstallProperties.DEFAULT_LOCAL_ARTIFACT_PATH;
        }
        artifactPath = artifactPath.trim();
        Path configured = Paths.get(artifactPath);
        if (!AgentInstallProperties.DEFAULT_LOCAL_ARTIFACT_PATH.equals(artifactPath)) {
            return existingFile(configured);
        }
        Path[] candidates = {
                configured,
                Paths.get("codeperf-agent", "target", AGENT_FILE_NAME),
                Paths.get("..", "codeperf-agent", "target", AGENT_FILE_NAME)
        };
        for (Path candidate : candidates) {
            Path existing = existingFile(candidate);
            if (existing != null) {
                return existing;
            }
        }
        return null;
    }

    private Path existingFile(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        return Files.isRegularFile(absolute) ? absolute : null;
    }
}
