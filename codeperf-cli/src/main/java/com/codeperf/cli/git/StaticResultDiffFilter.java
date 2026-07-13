package com.codeperf.cli.git;

import com.codeperf.analysis.staticanalysis.StaticFinding;
import com.codeperf.analysis.staticanalysis.StaticResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StaticResultDiffFilter {

    private StaticResultDiffFilter() {
    }

    public static StaticResult filter(StaticResult result, List<String> changedJavaFiles) {
        if (result == null) {
            return result;
        }
        if (changedJavaFiles == null || changedJavaFiles.isEmpty()) {
            return new StaticResult(result.getTargetPackage(), result.getClassesScanned(), new ArrayList<StaticFinding>());
        }
        Set<String> changed = normalize(changedJavaFiles);
        List<StaticFinding> findings = new ArrayList<>();
        for (StaticFinding finding : result.getFindings()) {
            String sourceFile = normalizePath(finding.getSourceFile());
            if (changed.contains(sourceFile)) {
                findings.add(finding);
            }
        }
        return new StaticResult(result.getTargetPackage(), result.getClassesScanned(), findings);
    }

    private static Set<String> normalize(List<String> paths) {
        Set<String> normalized = new HashSet<>();
        for (String path : paths) {
            normalized.add(normalizePath(path));
        }
        return normalized;
    }

    private static String normalizePath(String path) {
        return path == null ? "" : path.trim().replace('\\', '/');
    }
}
