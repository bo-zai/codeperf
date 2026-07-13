package com.codeperf.cli;

import com.beust.jcommander.JCommander;
import com.codeperf.cli.cmd.GateCommand;
import com.codeperf.cli.cmd.ReportCommand;
import com.codeperf.cli.cmd.ScanCommand;
import com.codeperf.cli.cmd.ScanDiffCommand;
import com.codeperf.cli.cmd.TaskCommand;

/**
 * CLI 入口：JCommander 装配子命令并分发。
 * 见 docs/03-cli.md 第 7 节。
 */
public class Main {

    public static void main(String[] args) {
        TaskCommand task = new TaskCommand();
        ReportCommand report = new ReportCommand();
        ScanCommand scan = new ScanCommand();
        ScanDiffCommand scanDiff = new ScanDiffCommand();
        GateCommand gate = new GateCommand();

        JCommander jc = JCommander.newBuilder()
                .programName("codeperf-cli")
                .addCommand("task", task)
                .addCommand("report", report)
                .addCommand("scan", scan)
                .addCommand("scan-diff", scanDiff)
                .addCommand("gate", gate)
                .build();

        if (args.length == 0) {
            jc.usage();
            System.exit(1);
        }

        jc.parse(args);
        String parsed = jc.getParsedCommand();

        int exitCode;
        if (parsed == null) {
            jc.usage();
            exitCode = 1;
        } else {
            try {
                switch (parsed) {
                    case "task":
                        exitCode = task.execute();
                        break;
                    case "report":
                        exitCode = report.execute();
                        break;
                    case "scan":
                        exitCode = scan.execute();
                        break;
                    case "scan-diff":
                        exitCode = scanDiff.execute();
                        break;
                    case "gate":
                        exitCode = gate.execute();
                        break;
                    default:
                        System.err.println("未知命令: " + parsed);
                        jc.usage();
                        exitCode = 1;
                }
            } catch (Exception e) {
                System.err.println("[codeperf] 执行失败: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                exitCode = 2;
            }
        }
        System.exit(exitCode);
    }
}
