package com.codeperf.analysis.source;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ForEachStmt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaAstParserTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_ParseFileAndKeepLineNumbers_When_SourceIsValid() throws Exception {
        Path source = tempDir.resolve("OrderService.java");
        Files.write(source, (
                "class OrderService {\n"
                        + "  void run() {\n"
                        + "    for (String id : ids()) {\n"
                        + "      mapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "  java.util.List<String> ids() { return java.util.Collections.emptyList(); }\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));

        CompilationUnit unit = new JavaAstParser().parse(source);

        assertTrue(unit.getClassByName("OrderService").isPresent());
        assertEquals(3, unit.findAll(ForEachStmt.class).get(0).getBegin().get().line);
    }
}
