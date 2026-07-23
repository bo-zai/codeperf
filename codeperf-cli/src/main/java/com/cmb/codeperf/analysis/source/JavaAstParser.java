package com.cmb.codeperf.analysis.source;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Java AST 解析器：封装 JavaParser 的静态解析方法。
 * <p>
 * 使用场景：SourceScanner 调用此类解析源码文件，获取 CompilationUnit 用于规则分析。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>单例复用：StaticJavaParser 内部共享配置，无需每次创建</li>
 *   <li>异常包装：将 ParseProblemException 包装为 IOException，便于统一处理</li>
 * </ul>
 */
public class JavaAstParser {

    public CompilationUnit parse(Path sourceFile) throws IOException {
        try {
            return StaticJavaParser.parse(sourceFile);
        } catch (ParseProblemException e) {
            throw new IOException("Java 源码解析失败: " + sourceFile + ", " + e.getMessage(), e);
        }
    }
}

