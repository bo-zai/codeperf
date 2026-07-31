package com.cmb.codeperf.server.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Git 仓库地址规范化工具。
 * 静态扫描和动态 agent 可能分别来自开发机与流水线，remoteUrl 协议可能不同，
 * 因此服务端必须使用稳定仓库键做关联，不能直接比较原始 URL。
 */
public final class RepositoryUrlNormalizer {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String GIT_SUFFIX = ".git";

    private RepositoryUrlNormalizer() {
    }

    /**
     * 将 SSH/HTTPS 等 Git 地址规范化为统一仓库键。
     *
     * @param remoteUrl Git 远程地址
     * @return 统一仓库键，例如 gitee.itc.cmbchina.cn/s992391/demo
     */
    public static String toRepoKey(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty() || UNKNOWN.equals(remoteUrl.trim())) {
            return "";
        }
        String value = trimTail(remoteUrl.trim().replace('\\', '/')).toLowerCase();
        String parsed = parseUriStyle(value);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return normalizeScpStyle(value);
    }

    /**
     * 规范化后仍保留路径结构，用于解析 namespace/repoName。
     *
     * @param remoteUrl Git 远程地址
     * @return 统一路径
     */
    public static String toNormalizedPath(String remoteUrl) {
        return toRepoKey(remoteUrl);
    }

    private static String parseUriStyle(String value) {
        if (!value.contains("://")) {
            return "";
        }
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || host.trim().isEmpty() || path == null || path.trim().isEmpty()) {
                return "";
            }
            return removeGitSuffix(trimSlashes(host) + "/" + trimSlashes(path));
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private static String normalizeScpStyle(String value) {
        int at = value.indexOf('@');
        if (at >= 0) {
            value = value.substring(at + 1);
        }
        int colon = value.indexOf(':');
        if (colon >= 0 && value.indexOf('/') > colon) {
            value = value.substring(0, colon) + "/" + value.substring(colon + 1);
        }
        return removeGitSuffix(trimSlashes(value));
    }

    private static String trimTail(String value) {
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int fragment = value.indexOf('#');
        if (fragment >= 0) {
            value = value.substring(0, fragment);
        }
        return trimSlashes(value);
    }

    private static String trimSlashes(String value) {
        String result = value == null ? "" : value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String removeGitSuffix(String value) {
        return value.endsWith(GIT_SUFFIX) ? value.substring(0, value.length() - GIT_SUFFIX.length()) : value;
    }
}
