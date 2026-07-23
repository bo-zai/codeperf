package com.cmb.codeperf.analysis.source.index;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class IndexedField {
    private final String className;
    private final String fieldName;
    private final String fieldType;
    private final Path sourceFile;
}

