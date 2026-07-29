package com.cmb.codeperf.cli.git;

/**
 * Git pre-push 钩子传入的推送范围。
 * Git hook 的标准输入能准确表达本次 push 的 old..new 范围，比配置里的默认 baseRef 更适合企业多人分支合并场景。
 */
public class GitPushRange {

    private static final String OLD_SHA = "CODEPERF_PUSH_OLD_SHA";
    private static final String NEW_SHA = "CODEPERF_PUSH_NEW_SHA";
    private static final String REMOTE_BRANCH = "CODEPERF_PUSH_REMOTE_BRANCH";
    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";

    private final String baseRef;
    private final String headRef;
    private final String remoteBranch;

    private GitPushRange(String baseRef, String headRef, String remoteBranch) {
        this.baseRef = baseRef;
        this.headRef = headRef;
        this.remoteBranch = remoteBranch;
    }

    /**
     * 从环境变量解析 push range。
     *
     * @param reader 环境变量读取器，测试中可替换
     * @return push range，缺少必要字段或删除远端分支时返回空对象
     */
    public static GitPushRange fromEnvironment(EnvironmentReader reader) {
        String oldSha = value(reader.get(OLD_SHA));
        String newSha = value(reader.get(NEW_SHA));
        if (oldSha.isEmpty() || newSha.isEmpty() || ZERO_SHA.equals(oldSha) || ZERO_SHA.equals(newSha)) {
            return empty();
        }
        return new GitPushRange(oldSha, newSha, value(reader.get(REMOTE_BRANCH)));
    }

    public static GitPushRange current() {
        return fromEnvironment(new EnvironmentReader() {
            @Override
            public String get(String name) {
                return System.getenv(name);
            }
        });
    }

    public static GitPushRange empty() {
        return new GitPushRange("", "", "");
    }

    public boolean isPresent() {
        return !baseRef.isEmpty() && !headRef.isEmpty();
    }

    public String getBaseRef() {
        return baseRef;
    }

    public String getHeadRef() {
        return headRef;
    }

    public String getRemoteBranch() {
        return remoteBranch;
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    public interface EnvironmentReader {
        String get(String name);
    }
}
