package com.cmb.codeperf.cli.git;

/**
 * 扫描基线：描述本次静态扫描实际采用的 Git 范围。
 * <p>
 * 设计意图：
 * <ul>
 *   <li>source 用于说明这次基线从哪里来，便于控制台和报告解释</li>
 *   <li>baseRef/headRef 直接喂给 diff 逻辑</li>
 *   <li>diffMode 允许在没有明确提交范围时退回工作区模式</li>
 * </ul>
 */
public class GitScanRange {

    private final String source;
    private final String baseRef;
    private final String headRef;
    private final String remoteBranch;
    private final String upstreamRef;
    private final String diffMode;

    public GitScanRange(String source, String baseRef, String headRef,
                        String remoteBranch, String upstreamRef, String diffMode) {
        this.source = value(source);
        this.baseRef = value(baseRef);
        this.headRef = value(headRef);
        this.remoteBranch = value(remoteBranch);
        this.upstreamRef = value(upstreamRef);
        this.diffMode = value(diffMode);
    }

    public String getSource() {
        return source;
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

    public String getUpstreamRef() {
        return upstreamRef;
    }

    public String getDiffMode() {
        return diffMode;
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }
}
