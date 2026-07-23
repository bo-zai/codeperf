package com.cmb.codeperf.cli.git;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 变更行集合：存储文件到变更行号的映射，支持快速判断发现是否属于变更范围。
 * <p>
 * 使用示例：
 * <pre>
 * ChangedLineSet changes = resolver.resolve(workingDir, base, head, mode);
 * if (changes.contains("src/main/java/Example.java", 42)) {
 *     // 发现位于变更行，标记为 NEW 或 MODIFIED
 * }
 * </pre>
 */
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

