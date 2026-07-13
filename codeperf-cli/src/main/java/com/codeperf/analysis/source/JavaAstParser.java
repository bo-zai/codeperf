package com.codeperf.analysis.source;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;

public class JavaAstParser {

    public CompilationUnit parse(Path sourceFile) throws IOException {
        try {
            return StaticJavaParser.parse(sourceFile);
        } catch (ParseProblemException e) {
            throw new IOException("Java 源码解析失败: " + sourceFile + ", " + e.getMessage(), e);
        }
    }
}
