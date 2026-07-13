package com.codeperf.analysis.source.index;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SourceClassIndexTest {

    @Test
    public void should_IndexFieldsAndMethods_When_CompilationUnitProvided() {
        CompilationUnit unit = StaticJavaParser.parse(
                "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport() { loadUser(); }\n"
                        + "  User loadUser() { return orderMapper.selectById(1L); }\n"
                        + "}\n");

        SourceClassIndex index = new SourceClassIndex();
        index.add(Paths.get("src/main/java/OrderService.java"), unit);

        assertEquals("OrderMapper", index.findFieldType("OrderService", "orderMapper").get());
        assertTrue(index.findMethod("OrderService", "loadUser").isPresent());
        assertEquals("OrderService", index.findMethod("OrderService", "loadUser").get().getClassName());
    }
}
