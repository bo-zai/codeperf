package com.codeperf.analysis.source.index;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 源码类索引：存储类的方法和字段信息，支持跨方法调用链追踪。
 * <p>
 * 索引结构：
 * <ul>
 *   <li>methods: Map&lt;类名#方法名, IndexedMethod&gt;</li>
 *   <li>fields: Map&lt;类名#字段名, IndexedField&gt;</li>
 * </ul>
 * <p>
 * 用途：
 * <ul>
 *   <li>调用链追踪：根据方法名查找方法声明，递归追踪到 I/O 调用</li>
 *   <li>类型推断：根据字段名查找字段类型，识别 Redis/HTTP/DB 客户端</li>
 * </ul>
 */
public class SourceClassIndex {

    private final Map<String, IndexedMethod> methods = new HashMap<>();
    private final Map<String, IndexedField> fields = new HashMap<>();

    public void add(Path sourceFile, CompilationUnit unit) {
        for (ClassOrInterfaceDeclaration clazz : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = clazz.getNameAsString();
            // 索引字段：用于推断方法调用的接收者类型（如 redisTemplate → RedisTemplate）
            for (FieldDeclaration field : clazz.getFields()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    fields.put(key(className, variable.getNameAsString()),
                            new IndexedField(className, variable.getNameAsString(),
                                    variable.getTypeAsString(), sourceFile));
                }
            }
            // 索引方法：用于调用链追踪时查找方法声明
            for (MethodDeclaration method : clazz.getMethods()) {
                methods.put(key(className, method.getNameAsString()),
                        new IndexedMethod(className, method.getNameAsString(), sourceFile, method));
            }
        }
    }

    public Optional<String> findFieldType(String className, String fieldName) {
        IndexedField field = fields.get(key(className, fieldName));
        return field == null ? Optional.empty() : Optional.of(field.getFieldType());
    }

    public Optional<IndexedMethod> findMethod(String className, String methodName) {
        return Optional.ofNullable(methods.get(key(className, methodName)));
    }

    private String key(String className, String memberName) {
        return className + "#" + memberName;
    }
}
