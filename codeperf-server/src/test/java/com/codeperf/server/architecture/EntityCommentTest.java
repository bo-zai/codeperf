package com.codeperf.server.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityCommentTest {

    @Test
    public void should_DocumentEntityFields_When_MappingMysqlTables() throws IOException {
        Path entityDirectory = Paths.get("src/main/java/com/codeperf/server/model/entity");
        List<Path> entityFiles;
        try (Stream<Path> files = Files.list(entityDirectory)) {
            entityFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }

        for (Path entityFile : entityFiles) {
            assertFieldComments(entityFile);
        }
    }

    private void assertFieldComments(Path entityFile) throws IOException {
        List<String> lines = Files.readAllLines(entityFile, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (!line.startsWith("private ")) {
                continue;
            }
            assertTrue(hasJavadocBefore(lines, index),
                    entityFile.getFileName() + " field missing comment: " + line);
        }
    }

    private boolean hasJavadocBefore(List<String> lines, int fieldIndex) {
        for (int index = fieldIndex - 1; index >= 0; index--) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("@")) {
                continue;
            }
            return line.endsWith("*/");
        }
        return false;
    }
}
