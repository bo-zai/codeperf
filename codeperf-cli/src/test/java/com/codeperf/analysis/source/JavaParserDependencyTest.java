package com.codeperf.analysis.source;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaParserDependencyTest {

    @Test
    public void should_ParseJavaSource_When_JavaParserDependencyAvailable() {
        CompilationUnit unit = StaticJavaParser.parse("class Demo { void run() {} }");

        assertEquals("Demo", unit.getClassByName("Demo").get().getNameAsString());
    }
}
