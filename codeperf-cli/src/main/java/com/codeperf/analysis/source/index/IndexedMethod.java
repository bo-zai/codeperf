package com.codeperf.analysis.source.index;

import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class IndexedMethod {
    private final String className;
    private final String methodName;
    private final Path sourceFile;
    private final MethodDeclaration declaration;
}
