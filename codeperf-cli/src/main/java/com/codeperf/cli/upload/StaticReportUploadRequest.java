package com.codeperf.cli.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传请求体：封装项目、Git 元数据和报告 JSON。
 * <p>
 * 服务端使用这些信息：
 * <ul>
 *   <li>project + env：隔离不同环境和项目的报告</li>
 *   <li>commit + branch：关联代码变更和趋势分析</li>
 *   <li>author/committer：问题分配和通知</li>
 *   <li>reportJson：报告内容，用于解析和展示</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public class StaticReportUploadRequest {
    private final String project;
    private final String env;
    private final String commit;
    private final String branch;
    private final String remoteUrl;
    private final String authorName;
    private final String authorEmail;
    private final String authorTime;
    private final String committerName;
    private final String committerEmail;
    private final String commitMessage;
    private final String reportJson;
}
