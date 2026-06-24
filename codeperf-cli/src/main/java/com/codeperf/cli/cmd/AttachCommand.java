package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.AnalysisFacade;
import com.codeperf.cli.attach.AttachHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * attach 子命令：attach→等采集→可选生成报告。见 docs/03-cli.md 第 2.1 节。
 */
@Parameters(commandDescription = "Attach codeperf agent to a running JVM and collect profile data")
public class AttachCommand {

    @Parameter(names = "--pid", required = true, description = "Target JVM process ID")
    private String pid;

    @Parameter(names = "--target-package", required = true, description = "Application package(s) to instrument, comma-separated")
    private String targetPackage;

    @Parameter(names = "--entry", required = true, description = "HTTP entry to match, e.g. 'POST /api/orders/report'")
    private String entry;

    @Parameter(names = "--agent", description = "Path to codeperf-agent.jar; auto-detected if omitted")
    private String agentPath;

    @Parameter(names = "--output", description = "Raw data output file")
    private String output = "perf-data.raw";

    @Parameter(names = "--slow-sql-ms", description = "Slow SQL threshold in milliseconds")
    private long slowSqlMs = 500;

    @Parameter(names = "--sample-ms", description = "Stack sampling interval in milliseconds")
    private long sampleMs = 10;

    @Parameter(names = "--mode", description = "Collection mode: session (first match, default) or duration (TODO)")
    private String mode = "session";

    @Parameter(names = "--wait", description = "Max seconds to wait for collection completion")
    private int waitSec = 120;

    @Parameter(names = "--report", description = "Generate this HTML report after collection")
    private String reportOutput;

    public int execute() throws Exception {
        Path outputPath = Paths.get(output).toAbsolutePath();
        // 1. 清理旧输出
        Files.deleteIfExists(outputPath);
        Path done = Paths.get(outputPath.toString() + ".done");
        Files.deleteIfExists(done);

        // 2. 拼装 agent 参数
        StringBuilder args = new StringBuilder();
        args.append("targetPackage=").append(targetPackage);
        args.append(";entry=").append(entry);
        args.append(";slowSqlMs=").append(slowSqlMs);
        args.append(";output=").append(outputPath);
        args.append(";sampleMs=").append(sampleMs);
        args.append(";mode=").append(mode);

        // 3. 定位 agent jar
        String agentJar = agentPath != null ? agentPath : detectAgentJar();
        if (agentJar == null) {
            throw new IllegalStateException(
                    "找不到 codeperf-agent.jar。请用 --agent 指定。探测过：同目录 codeperf-agent.jar、"
                            + "codeperf-agent/target/codeperf-agent.jar。");
        }
        File af = new File(agentJar);
        if (!af.isFile()) {
            throw new IllegalStateException("agent jar 不存在: " + af.getAbsolutePath());
        }

        // 4. attach
        System.out.println("[codeperf] attaching to pid=" + pid);
        System.out.println("[codeperf] agent: " + af.getAbsolutePath());
        System.out.println("[codeperf] entry: " + entry);
        System.out.println("[codeperf] output: " + outputPath);
        try {
            AttachHelper.attach(pid, af.getAbsolutePath(), args.toString());
        } catch (Exception e) {
            throw new IllegalStateException("attach 失败。请确认：① 用 JDK8 运行 CLI；② pid 正确；"
                    + "③ 目标 JVM 为 JDK8。错误: " + e, e);
        }

        // 5. 等待采集完成
        System.out.println("[codeperf] agent 已挂载，请手动发请求到 " + entry);
        System.out.println("[codeperf] 等待采集完成...（" + waitSec + "s 超时）");
        boolean completed = waitForDone(done);
        if (!completed) {
            System.err.println("[codeperf] 等待超时，采集未完成。");
            return 1;
        }
        System.out.println("[codeperf] 采集完成，原始数据位于 " + outputPath);

        // 6. 可选生成报告
        if (reportOutput != null) {
            System.out.println("[codeperf] 生成报告到 " + reportOutput);
            return AnalysisFacade.analyze(outputPath.toString(), reportOutput, 0);
        }
        return 0;
    }

    private String detectAgentJar() {
        // 同目录
        File here = new File("codeperf-agent.jar");
        if (here.isFile()) {
            return here.getAbsolutePath();
        }
        // 开发期布局
        File dev = new File("codeperf-agent/target/codeperf-agent.jar");
        if (dev.isFile()) {
            return dev.getAbsolutePath();
        }
        // 与 cli jar 同目录
        String cliJar = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if (cliJar != null) {
            File sibling = new File(new File(cliJar).getParent(), "codeperf-agent.jar");
            if (sibling.isFile()) {
                return sibling.getAbsolutePath();
            }
        }
        return null;
    }

    private boolean waitForDone(Path done) {
        long start = System.currentTimeMillis();
        long deadline = start + waitSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(done)) {
                return true;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
