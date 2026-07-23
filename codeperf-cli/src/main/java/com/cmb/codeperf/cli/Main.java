package com.cmb.codeperf.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.cmb.codeperf.cli.cmd.DoctorCommand;
import com.cmb.codeperf.cli.cmd.InitCommand;
import com.cmb.codeperf.cli.cmd.InstallHooksCommand;
import com.cmb.codeperf.cli.cmd.ScanCommand;

/**
 * CLI 入口：JCommander 装配子命令并分发。
 * 见 docs/03-cli.md 第 7 节。
 */
public class Main {

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * 执行 CLI 命令并返回退出码。
     *
     * @param args 命令行参数
     * @return 进程退出码，0 表示成功
     */
    public static int run(String[] args) {
        InitCommand init = new InitCommand();
        ScanCommand scan = new ScanCommand();
        DoctorCommand doctor = new DoctorCommand();
        InstallHooksCommand installHooks = new InstallHooksCommand();

        JCommander jc = JCommander.newBuilder()
                .programName("codeperf")
                .addCommand("init", init)
                .addCommand("scan", scan)
                .addCommand("doctor", doctor)
                .addCommand("install-hooks", installHooks)
                .build();

        if (args.length == 0) {
            jc.usage();
            return 1;
        }

        if (isHelp(args[0])) {
            jc.usage();
            return 0;
        }

        if (args.length == 2 && isHelp(args[1]) && jc.getCommands().containsKey(args[0])) {
            System.out.println("Usage: codeperf " + args[0]);
            jc.getCommands().get(args[0]).usage();
            return 0;
        }

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println("[codeperf] 参数错误: " + e.getMessage());
            jc.usage();
            return 1;
        }
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
        return exitCode;
    }

    private static boolean isHelp(String arg) {
        return "--help".equals(arg) || "-h".equals(arg);
    }
}

