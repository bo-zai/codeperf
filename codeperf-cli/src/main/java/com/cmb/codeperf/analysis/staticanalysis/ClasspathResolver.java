package com.cmb.codeperf.analysis.staticanalysis;

import java.io.File;

/**
 * 自动解析编译产物目录（Maven target/classes、Gradle build/classes、IDE out/）。
 * 见 docs/05-static-analysis.md 第 5、6 节。
 */
public final class ClasspathResolver {

    private static final String[] CANDIDATES = {
            "target/classes",
            "build/classes/java/main",
            "out/production/classes",
            "bin",
    };

    private ClasspathResolver() {}

    /**
     * @param hint 用户通过 --classes-dir 指定的目录，可为 null
     * @return 存在的 classes 目录，找不到返回 null
     */
    public static File resolve(String hint) {
        if (hint != null && !hint.trim().isEmpty()) {
            File f = new File(hint);
            return f.exists() ? f : null;
        }
        File base = new File(".");
        for (String c : CANDIDATES) {
            File f = new File(base, c);
            if (f.isDirectory()) return f;
        }
        return null;
    }
}

