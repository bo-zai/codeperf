package com.codeperf.cli.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codeperf.analysis.AnalysisFacade;

/**
 * report 子命令：读取原始数据 JSON，生成 HTML 报告并返回门禁退出码。
 * 见 docs/03-cli.md 第 2.2 节。
 */
@Parameters(commandDescription = "Generate HTML report from collected profile data")
public class ReportCommand {

    @Parameter(names = "--input", description = "Raw data JSON file from agent")
    private String input = "perf-data.raw";

    @Parameter(names = "--static", description = "Static scan result JSON (perf-static.json) for cross-validation")
    private String staticInput;

    @Parameter(names = "--output", description = "HTML report output file")
    private String output = "perf-report.html";

    @Parameter(names = "--fail-on", description = "Gate threshold: none(0), info(1), warn(2), critical(3)")
    private String failOn = "none";

    public int execute() {
        int threshold = parseFailOn(failOn);
        System.out.println("[codeperf] 分析原始数据: " + input);
        if (staticInput != null && !staticInput.trim().isEmpty()) {
            System.out.println("[codeperf] 交叉验证静态结果: " + staticInput);
        }
        System.out.println("[codeperf] 生成报告: " + output);
        System.out.println("[codeperf] 门禁阈值: " + failOn + " (" + threshold + ")");
        return AnalysisFacade.analyze(input, staticInput, output, threshold);
    }

    private int parseFailOn(String s) {
        if (s == null) return 0;
        switch (s.toLowerCase()) {
            case "info":     return 1;
            case "warn":     return 2;
            case "critical": return 3;
            default:         return 0;
        }
    }
}
