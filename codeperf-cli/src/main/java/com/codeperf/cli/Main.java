package com.codeperf.cli;

import com.beust.jcommander.JCommander;
import com.codeperf.cli.cmd.DoctorCommand;
import com.codeperf.cli.cmd.InitCommand;
import com.codeperf.cli.cmd.InstallHooksCommand;
import com.codeperf.cli.cmd.ScanCommand;

/**
 * CLI 入口：JCommander 装配子命令并分发。
 * 见 docs/03-cli.md 第 7 节。
 */
public class Main {

    public static void main(String[] args) {
        InitCommand init = new InitCommand();
        ScanCommand scan = new ScanCommand();
        DoctorCommand doctor = new DoctorCommand();
        InstallHooksCommand installHooks = new InstallHooksCommand();

        JCommander jc = JCommander.newBuilder()
                .programName("codeperf-cli")
                .addCommand("init", init)
                .addCommand("scan", scan)
                .addCommand("doctor", doctor)
                .addCommand("install-hooks", installHooks)
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
                    case "init":
                        exitCode = init.execute();
                        break;
                    case "scan":
                        exitCode = scan.execute();
                        break;
                    case "doctor":
                        exitCode = doctor.execute();
                        break;
                    case "install-hooks":
                        exitCode = installHooks.execute();
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
