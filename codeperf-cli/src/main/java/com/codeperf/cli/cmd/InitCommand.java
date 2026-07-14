package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Parameters(commandDescription = "Initialize CodePerf local config")
public class InitCommand {

    private Path workingDirectory;

    public int execute() {
        try {
            Path start = workingDirectory == null ? Paths.get(".").toAbsolutePath().normalize() : workingDirectory;
            Path root = findGitRoot(start);
            String projectName = inferProjectName(root);
            Files.createDirectories(root.resolve(".codeperf"));
            writeIfAbsent(root.resolve(".codeperf.yml"), defaultConfig(projectName));
            writeIfAbsent(root.resolve(".codeperf/agent.yml"), defaultAgent());
            System.out.println("[codeperf] init 完成，已存在的配置文件不会被覆盖");
            System.out.println("[codeperf] 请手工配置 JVM 参数: -javaagent:/opt/codeperf/codeperf-agent.jar="
                    + root.resolve(".codeperf/agent.yml").toAbsolutePath().normalize());
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
        String remoteUrl = readOriginUrl(root);
        String remoteProject = parseProjectName(remoteUrl);
        if (remoteProject != null && !remoteProject.isEmpty()) {
            return remoteProject;
        }
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

    private String defaultConfig(String projectName) {
        return "project: " + projectName + "\n"
                + "staticScan:\n"
                + "  enabled: true\n"
                + "  mode: changed\n"
                + "  sourceRoots:\n"
                + "    - src/main/java\n"
                + "  includeTests: false\n"
                + "  baseRef: origin/master\n"
                + "  headRef: HEAD\n"
                + "  failOn: WARN\n"
                + "  callChain:\n"
                + "    enabled: true\n"
                + "    maxDepth: 2\n"
                + "agent:\n"
                + "  enabled: true\n"
                + "  serverUrl: http://codeperf.company.com\n"
                + "  configPath: .codeperf/agent.yml\n"
                + "  jarPath: /opt/codeperf/codeperf-agent.jar\n"
                + "report:\n"
                + "  local:\n"
                + "    enabled: true\n"
                + "    path: .codeperf/report/source-report.json\n"
                + "  upload:\n"
                + "    enabled: false\n"
                + "    serverUrl: http://codeperf.company.com\n";
    }

    private String defaultAgent() {
        return "serverUrl: http://codeperf.company.com\n"
                + "analysisTaskId: ${CODEPERF_ANALYSIS_TASK_ID}\n"
                + "uploadEnabled: true\n"
                + "targetPackages: []\n"
                + "sampleMs: 10\n"
                + "mode: session\n";
    }
}
