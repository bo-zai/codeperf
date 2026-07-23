package com.cmb.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 初始化命令：在 Git 仓库根目录生成 {@code .codeperf.yml} 配置文件模板。
 * <p>
 * 设计决策：
 * <ul>
 *   <li>幂等性：若配置文件已存在则跳过，避免覆盖用户定制配置</li>
 *   <li>项目名推断：优先从 git remote origin URL 解析，失败则使用目录名</li>
 *   <li>Git 仓库约束：必须在 Git 仓库内执行，因为扫描依赖 git diff 获取变更文件</li>
 * </ul>
 * 使用示例：{@code codeperf init}
 */
@Parameters(commandDescription = "Initialize CodePerf local config")
public class InitCommand {

    private Path workingDirectory;

    /**
     * 执行初始化命令。
     *
     * @return 退出码：0 成功，2 执行异常（非 Git 仓库、IO 错误等）
     */
    public int execute() {
        try {
            Path start = workingDirectory == null ? Paths.get(".").toAbsolutePath().normalize() : workingDirectory;
            Path root = findGitRoot(start);
            String projectName = inferProjectName(root);
            List<String> sourceRoots = discoverSourceRoots(root);
            writeIfAbsent(root.resolve(".codeperf.yml"), defaultConfig(projectName, sourceRoots));
            System.out.println("[codeperf] init 完成，已存在的配置文件不会被覆盖");
            System.out.println("[codeperf] 如需接入 Git pre-push，请执行: codeperf install-hooks");
            return 0;
        } catch (Exception e) {
            System.err.println("[codeperf] init 失败: " + e.getMessage());
            return 2;
        }
    }

    void setWorkingDirectoryForTest(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    private void writeIfAbsent(Path path, String content) throws Exception {
        if (Files.exists(path)) {
            System.out.println("[codeperf] 已存在，跳过: " + path);
            return;
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("[codeperf] 已生成: " + path);
    }

    private Path findGitRoot(Path start) throws Exception {
        Path current = Files.isDirectory(start) ? start : start.getParent();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current.toAbsolutePath().normalize();
            }
            current = current.getParent();
        }
        throw new IllegalStateException("未找到 Git 根目录，请在 Git 仓库内执行");
    }

    private String inferProjectName(Path root) throws Exception {
        // 优先从 git remote origin URL 解析项目名（支持 SSH/HTTPS 格式）
        String remoteUrl = readOriginUrl(root);
        String remoteProject = parseProjectName(remoteUrl);
        if (remoteProject != null && !remoteProject.isEmpty()) {
            return remoteProject;
        }
        // 回退到目录名（适用于本地仓库或无 remote 的场景）
        Path fileName = root.getFileName();
        return fileName == null ? "codeperf-project" : fileName.toString();
    }

    private String readOriginUrl(Path root) throws Exception {
        Path gitConfig = root.resolve(".git/config");
        if (!Files.isRegularFile(gitConfig)) {
            return null;
        }
        List<String> lines = Files.readAllLines(gitConfig, StandardCharsets.UTF_8);
        boolean inOrigin = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                inOrigin = "[remote \"origin\"]".equals(trimmed);
                continue;
            }
            if (inOrigin && trimmed.startsWith("url")) {
                int index = trimmed.indexOf('=');
                if (index >= 0) {
                    return trimmed.substring(index + 1).trim();
                }
            }
        }
        return null;
    }

    private String parseProjectName(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            return null;
        }
        // 规范化路径分隔符，移除 .git 后缀
        String normalized = remoteUrl.trim().replace('\\', '/');
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        int slash = normalized.lastIndexOf('/');
        int colon = normalized.lastIndexOf(':');
        int separator = Math.max(slash, colon);
        String name = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        return name.trim().isEmpty() ? null : name.trim();
    }

    private List<String> discoverSourceRoots(Path root) throws Exception {
        List<String> sourceRoots = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            sourceRoots = paths
                    .filter(Files::isDirectory)
                    .filter(path -> isMainJavaSourceRoot(root, path))
                    .map(path -> toUnixRelativePath(root, path))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (sourceRoots.isEmpty()) {
            // 没有发现源码目录时保留旧默认值，让新仓库或尚未拉取源码的项目仍可初始化。
            sourceRoots.add("src/main/java");
        }
        return sourceRoots;
    }

    private boolean isMainJavaSourceRoot(Path root, Path path) {
        Path relative = root.relativize(path.toAbsolutePath().normalize());
        if (hasExcludedSegment(relative)) {
            return false;
        }
        int count = relative.getNameCount();
        return count >= 3
                && "src".equals(relative.getName(count - 3).toString())
                && "main".equals(relative.getName(count - 2).toString())
                && "java".equals(relative.getName(count - 1).toString());
    }

    private boolean hasExcludedSegment(Path relative) {
        for (Path part : relative) {
            String name = part.toString();
            if (".git".equals(name)
                    || "target".equals(name)
                    || "build".equals(name)
                    || ".idea".equals(name)
                    || ".gradle".equals(name)
                    || "node_modules".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String toUnixRelativePath(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private String defaultConfig(String projectName, List<String> sourceRoots) {
        List<String> roots = sourceRoots == null || sourceRoots.isEmpty()
                ? Collections.singletonList("src/main/java")
                : sourceRoots;
        StringBuilder builder = new StringBuilder();
        builder.append("project: ").append(projectName).append("\n")
                .append("staticScan:\n")
                .append("  enabled: true\n")
                .append("  mode: changed\n")
                .append("  sourceRoots:\n");
        for (String sourceRoot : roots) {
            builder.append("    - ").append(sourceRoot).append("\n");
        }
        builder.append("  includeTests: false\n")
                .append("  baseRef: origin/master\n")
                .append("  headRef: HEAD\n")
                .append("  failOn: WARN\n")
                .append("  callChain:\n")
                .append("    enabled: true\n")
                .append("    maxDepth: 2\n");
        appendModules(builder, roots);
        builder.append("report:\n")
                .append("  local:\n")
                .append("    enabled: true\n")
                .append("    path: .codeperf/report/source-report.json\n")
                .append("  upload:\n")
                .append("    enabled: false\n")
                .append("    serverUrl: http://codeperf.company.com\n");
        return builder.toString();
    }

    private void appendModules(StringBuilder builder, List<String> sourceRoots) {
        if (sourceRoots.size() <= 1 && "src/main/java".equals(sourceRoots.get(0))) {
            return;
        }
        builder.append("modules:\n");
        for (String sourceRoot : sourceRoots) {
            builder.append("  - name: ").append(moduleName(sourceRoot)).append("\n")
                    .append("    sourceRoots:\n")
                    .append("      - ").append(sourceRoot).append("\n");
        }
    }

    private String moduleName(String sourceRoot) {
        String suffix = "/src/main/java";
        if (sourceRoot.endsWith(suffix)) {
            String modulePath = sourceRoot.substring(0, sourceRoot.length() - suffix.length());
            int slash = modulePath.lastIndexOf('/');
            return slash >= 0 ? modulePath.substring(slash + 1) : modulePath;
        }
        return sourceRoot;
    }

}

