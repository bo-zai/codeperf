package com.cmb.codeperf.server.controller;

import com.cmb.codeperf.server.config.AgentInstallProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AgentArtifactControllerTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_ReturnAgentJar_When_ConfiguredArtifactExists() throws Exception {
        Path artifact = tempDir.resolve("codeperf-agent.jar");
        Files.write(artifact, "fake-agent".getBytes(StandardCharsets.UTF_8));
        AgentInstallProperties properties = new AgentInstallProperties();
        properties.setLocalArtifactPath(artifact.toString());

        ResponseEntity<Resource> response = new AgentArtifactController(properties).download();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("fake-agent", read(response.getBody()));
    }

    @Test
    public void should_ReturnNotFound_When_ConfiguredArtifactMissing() {
        AgentInstallProperties properties = new AgentInstallProperties();
        properties.setLocalArtifactPath(tempDir.resolve("missing.jar").toString());

        ResponseEntity<Resource> response = new AgentArtifactController(properties).download();

        assertEquals(404, response.getStatusCodeValue());
    }

    private String read(Resource resource) throws Exception {
        byte[] bytes = new byte[(int) resource.contentLength()];
        try (InputStream input = resource.getInputStream()) {
            int read = input.read(bytes);
            assertEquals(bytes.length, read);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

