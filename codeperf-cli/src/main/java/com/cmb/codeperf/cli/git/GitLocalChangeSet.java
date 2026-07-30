package com.cmb.codeperf.cli.git;

import lombok.Getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 本地预检变更集合。
 * <p>
 * 用于描述开发者手动执行 {@code codeperf pre-scan} 时实际会扫描的文件来源。
 */
@Getter
public class GitLocalChangeSet {

    private final List<Path> sourceFiles;
    private final int committedNotPushedCount;
    private final int stagedCount;
    private final int unstagedCount;
    private final int untrackedCount;

    public GitLocalChangeSet(List<Path> sourceFiles, int committedNotPushedCount,
                             int stagedCount, int unstagedCount, int untrackedCount) {
        this.sourceFiles = sourceFiles == null
                ? Collections.<Path>emptyList()
                : Collections.unmodifiableList(new ArrayList<Path>(sourceFiles));
        this.committedNotPushedCount = committedNotPushedCount;
        this.stagedCount = stagedCount;
        this.unstagedCount = unstagedCount;
        this.untrackedCount = untrackedCount;
    }
}
