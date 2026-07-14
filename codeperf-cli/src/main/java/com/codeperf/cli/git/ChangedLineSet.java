package com.codeperf.cli.git;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ChangedLineSet {
    private final Map<String, Set<Integer>> changedLines = new LinkedHashMap<>();

    public void add(String file, int line) {
        changedLines.computeIfAbsent(normalize(file), ignored -> new LinkedHashSet<>()).add(line);
    }

    public boolean contains(String file, int line) {
        Set<Integer> lines = changedLines.get(normalize(file));
        return lines != null && lines.contains(line);
    }

    public boolean containsAny(String file, int... lines) {
        for (int line : lines) {
            if (contains(file, line)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Set<Integer>> asMap() {
        return Collections.unmodifiableMap(changedLines);
    }

    private String normalize(String file) {
        return file == null ? "" : file.replace('\\', '/');
    }
}
