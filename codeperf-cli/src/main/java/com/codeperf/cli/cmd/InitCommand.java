package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandDescription = "Initialize CodePerf local config")
public class InitCommand {

    private Path workingDirectory;

    public int execute() {
        try {
            Path root = workingDirectory == null ? Paths.get(".").toAbsolutePath().normalize() : workingDirectory;
            Files.createDirectories(root.resolve(".codeperf"));
            Files.write(root.resolve(".codeperf.yml"), defaultConfig().getBytes(StandardCharsets.UTF_8));
            Files.write(root.resolve(".codeperf/agent.yml"), defaultAgent().getBytes(StandardCharsets.UTF_8));
            System.out.println("[codeperf] 已生成 .codeperf.yml 和 .codeperf/agent.yml");
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

    private String defaultConfig() {
        return "project: demo\n"
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
                + "  jarPath: /opt/codeperf/codeperf-agent.jar\n";
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
