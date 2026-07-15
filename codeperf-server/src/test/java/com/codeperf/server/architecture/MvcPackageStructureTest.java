package com.codeperf.server.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MvcPackageStructureTest {

    private static final Path SERVER_PACKAGE = Paths.get("src/main/java/com/codeperf/server");

    @Test
    public void should_UseThreeLayerMvcPackages_When_ServerCodeIsOrganized() {
        assertDirectoryExists("controller");
        assertDirectoryExists("service");
        assertDirectoryExists("service/impl");
        assertDirectoryExists("mapper");
        assertDirectoryExists("model/entity");
        assertDirectoryExists("model/dto/request");
        assertDirectoryExists("model/dto/response");
        assertDirectoryExists("model/bo");
        assertDirectoryExists("common");
    }

    @Test
    public void should_NotKeepOldLayeredPackages_When_UsingMvcArchitecture() {
        assertDirectoryMissing("api");
        assertDirectoryMissing("application");
        assertDirectoryMissing("domain");
        assertDirectoryMissing("infrastructure");
    }

    private void assertDirectoryExists(String relativePath) {
        assertTrue(Files.isDirectory(SERVER_PACKAGE.resolve(relativePath)),
                "missing MVC package directory: " + relativePath);
    }

    private void assertDirectoryMissing(String relativePath) {
        assertFalse(Files.exists(SERVER_PACKAGE.resolve(relativePath)),
                "old layered package should be removed: " + relativePath);
    }
}
