package com.codeperf.cli.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScanCommandTest {

    @TempDir
    private Path tempDir;

    @Test
    public void should_RunAstScan_When_ConfigAndChangedFileProvided() throws Exception {
        initGitRepo();
        write(".codeperf.yml",
                "project: demo\n"
                        + "staticScan:\n"
                        + "  sourceRoots:\n"
                        + "    - src/main/java\n"
                        + "  baseRef: HEAD\n"
                        + "  headRef: --\n"
                        + "  failOn: WARN\n");
        write("src/main/java/com/acme/OrderService.java",
                "package com.acme;\n"
                        + "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        runGit("add", "src/main/java/com/acme/OrderService.java");

        ScanCommand command = new ScanCommand();
        command.setWorkingDirectoryForTest(tempDir);
        command.setOutputForTest(".codeperf/report/source-report.json");

        int exitCode = command.execute();

        assertEquals(1, exitCode);
        assertTrue(Files.isRegularFile(tempDir.resolve(".codeperf/report/source-report.json")));
    }

    private void initGitRepo() throws Exception {
        runGit("init");
        runGit("config", "user.email", "codeperf@example.com");
        runGit("config", "user.name", "CodePerf Test");
        write("README.md", "# demo\n");
        runGit("add", ".");
        runGit("commit", "-m", "init");
    }

    private void write(String file, String content) throws Exception {
        Path path = tempDir.resolve(file);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private void runGit(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("git command failed");
        }
    }
}
