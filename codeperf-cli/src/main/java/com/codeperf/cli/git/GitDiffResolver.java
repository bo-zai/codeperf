package com.codeperf.cli.git;

import java.util.ArrayList;
import java.util.List;

public final class GitDiffResolver {

    private GitDiffResolver() {
    }

    public static List<String> parseChangedJavaFiles(List<String> nameOnlyOutput) {
        List<String> files = new ArrayList<>();
        if (nameOnlyOutput == null) {
            return files;
        }
        for (String line : nameOnlyOutput) {
            if (line == null) {
                continue;
            }
            String path = line.trim().replace('\\', '/');
            if (path.endsWith(".java")) {
                files.add(path);
            }
        }
        return files;
    }
}
