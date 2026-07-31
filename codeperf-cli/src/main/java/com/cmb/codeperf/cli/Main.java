package com.cmb.codeperf.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.cmb.codeperf.cli.cmd.DoctorCommand;
import com.cmb.codeperf.cli.cmd.HookPolicyCommand;
import com.cmb.codeperf.cli.cmd.InitCommand;
import com.cmb.codeperf.cli.cmd.InstallHooksCommand;
import com.cmb.codeperf.cli.cmd.PreScanCommand;
import com.cmb.codeperf.cli.cmd.ScanCommand;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * CLI 入口：JCommander 装配子命令并分发。
 * 见 docs/03-cli.md 第 7 节。
 */
public class Main {

    public static void main(String[] args) {
        setConsoleEncoding();
        System.exit(run(args));
    }

    /**
     * 设置控制台输出编码，解决 Windows 下中文乱码问题。
     * <ul>
     *   <li>Git Bash / IDEA Terminal：期望 UTF-8，需要强制设置</li>
     *   <li>PowerShell / CMD：使用系统默认编码（GBK），不应强制 UTF-8</li>
     * </ul>
     */
    private static void setConsoleEncoding() {
        if (isWindows() && isGitBashOrCygwin()) {
            try {
                System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
                System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /**
     * 检测是否运行在 Git Bash 或 Cygwin 环境。
     * 这些终端会设置特定的环境变量。
     */
    private static boolean isGitBashOrCygwin() {
        String term = System.getenv("TERM");
        String msystem = System.getenv("MSYSTEM");
        String cygwin = System.getenv("CYGWIN");

        return (term != null && term.contains("xterm"))
            || msystem != null
            || cygwin != null;
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
        PreScanCommand preScan = new PreScanCommand();
        DoctorCommand doctor = new DoctorCommand();
        InstallHooksCommand installHooks = new InstallHooksCommand();
        HookPolicyCommand hookPolicy = new HookPolicyCommand();

        JCommander jc = JCommander.newBuilder()
                .programName("codeperf")
                .addCommand("init", init)
                .addCommand("scan", scan)
                .addCommand("pre-scan", preScan)
                .addCommand("doctor", doctor)
                .addCommand("install-hooks", installHooks)
                .addCommand("hook-policy", hookPolicy)
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
                    case "pre-scan":
                        exitCode = preScan.execute();
                        break;
                    case "doctor":
                        exitCode = doctor.execute();
                        break;
                    case "install-hooks":
                        exitCode = installHooks.execute();
                        break;
                    case "hook-policy":
                        exitCode = hookPolicy.execute();
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

