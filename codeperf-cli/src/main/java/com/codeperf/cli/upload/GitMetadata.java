package com.codeperf.cli.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Git 元数据：封装 commit、branch、author 等信息，用于报告上传。
 * <p>
 * 用途：
 * <ul>
 *   <li>关联报告与代码变更：通过 commit 追踪报告对应的代码版本</li>
 *   <li>问题分配：通过 author 信息确定问题负责人</li>
 *   <li>趋势分析：通过 branch 和 commit 建立历史趋势</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public class GitMetadata {
    private final String commit;
    private final String branch;
    private final String remoteUrl;
    private final String authorName;
    private final String authorEmail;
    private final String authorTime;
    private final String committerName;
    private final String committerEmail;
    private final String commitMessage;
}
