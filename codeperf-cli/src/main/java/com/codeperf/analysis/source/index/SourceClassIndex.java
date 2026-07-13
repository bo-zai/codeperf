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

public class SourceClassIndex {

    private final Map<String, IndexedMethod> methods = new HashMap<>();
    private final Map<String, IndexedField> fields = new HashMap<>();

    public void add(Path sourceFile, CompilationUnit unit) {
        for (ClassOrInterfaceDeclaration clazz : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = clazz.getNameAsString();
            for (FieldDeclaration field : clazz.getFields()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    fields.put(key(className, variable.getNameAsString()),
                            new IndexedField(className, variable.getNameAsString(),
                                    variable.getTypeAsString(), sourceFile));
                }
            }
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
