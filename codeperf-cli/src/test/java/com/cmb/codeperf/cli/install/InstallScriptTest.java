package com.cmb.codeperf.cli.install;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstallScriptTest {

    private static final byte[] AGENT_BYTES = "fake-codeperf-agent".getBytes(StandardCharsets.UTF_8);

    @TempDir
    private Path tempDir;

    private HttpServer server;
    private String lastConfigRequestBody;

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void should_GenerateAgentFilesAndInjectDockerfile_When_RequiredEnvProvided() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "COPY target/app.jar /app/app.jar",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult first = runInstallScript(configUrl);

        assertEquals(0, first.exitCode, first.output);
        Path codeperfDir = tempDir.resolve("target/codeperf");
        assertTrue(Files.isRegularFile(codeperfDir.resolve("codeperf-agent.jar")));
        assertTrue(Files.isRegularFile(codeperfDir.resolve("agent.yml")));
        assertTrue(Files.isRegularFile(codeperfDir.resolve("build-info.properties")));
        assertTrue(Files.isRegularFile(tempDir.resolve("Dockerfile.codeperf.bak")));

        String dockerfile = readUtf8(tempDir.resolve("Dockerfile"));
        assertEquals(1, count(dockerfile, "CODEPERF_AGENT_START"));
        assertEquals(1, count(dockerfile, "CODEPERF_AGENT_END"));
        assertTrue(dockerfile.contains("COPY target/codeperf/ /opt/codeperf/"));
        assertTrue(dockerfile.contains("-javaagent:/opt/codeperf/codeperf-agent.jar=/opt/codeperf/agent.yml"));
        assertTrue(dockerfile.contains("ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"));

        String agentConfig = readUtf8(codeperfDir.resolve("agent.yml"));
        assertTrue(agentConfig.contains("serverUrl: http://codeperf-server:9095"));
        assertTrue(agentConfig.contains("  - com.demo.app"));
        assertTrue(agentConfig.contains("  - com.demo.common"));
        assertTrue(agentConfig.contains("excludedPackages:"));
        assertTrue(agentConfig.contains("  - com.cmb.cjtz"));
        assertTrue(agentConfig.contains("  - com.cmb.checkerframework"));
        assertTrue(agentConfig.contains("  - com.cmb.bee"));
        assertTrue(agentConfig.contains("  - com.cmbchina.ugw"));
        assertTrue(agentConfig.contains("connectTimeoutMs: 5000"));
        assertTrue(agentConfig.contains("readTimeoutMs: 60000"));

        String buildInfo = readUtf8(codeperfDir.resolve("build-info.properties"));
        assertTrue(buildInfo.contains("remoteUrl=git@gitlab.example.com:demo/demo-app.git"));
        assertTrue(buildInfo.contains("project=demo-app"));
        assertTrue(buildInfo.contains("branch=master"));
        assertTrue(buildInfo.contains("authorEmail=developer@example.com"));
        assertTrue(lastConfigRequestBody.contains("\"remoteUrl\":\"git@gitlab.example.com:demo/demo-app.git\""));
        assertTrue(lastConfigRequestBody.contains("\"project\":\"demo-app\""));

        ScriptResult second = runInstallScript(configUrl);

        assertEquals(0, second.exitCode, second.output);
        String dockerfileAfterSecondRun = readUtf8(tempDir.resolve("Dockerfile"));
        assertEquals(1, count(dockerfileAfterSecondRun, "CODEPERF_AGENT_START"));
        assertEquals(1, count(dockerfileAfterSecondRun, "COPY target/codeperf/ /opt/codeperf/"));
    }

    @Test
    public void should_InferThreeSegmentTargetPackage_When_SourceDirectoryExists() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        Path javaFile = tempDir.resolve("src/main/java/com/cmb/music/OrderService.java");
        Files.createDirectories(javaFile.getParent());
        Files.write(javaFile, Arrays.asList(
                "package com.cmb.music;",
                "public class OrderService { }"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertTrue(result.output.contains("已根据 src/main/java 目录推断 targetPackages=com.cmb.music"), result.output);
        assertTrue(agentConfig.contains("  - com.cmb.music"));
        assertFalse(agentConfig.contains("  - com.demo.app"));
    }

    @Test
    public void should_InferPackageTreeBoundaries_When_SourceContainsMultiplePackagePaths() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        writeJavaSource("src/main/java/com/cmb/music/app/OrderService.java", "com.cmb.music.app");
        writeJavaSource("src/main/java/com/cmb/music/common/MusicCommon.java", "com.cmb.music.common");
        writeJavaSource("src/main/java/cn/job/worker/JobWorker.java", "cn.job.worker");
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertTrue(result.output.contains("已根据 src/main/java 目录推断 targetPackages="), result.output);
        assertTrue(result.output.contains("com.cmb.music"), result.output);
        assertTrue(result.output.contains("cn.job.worker"), result.output);
        assertTrue(agentConfig.contains("  - com.cmb.music"));
        assertTrue(agentConfig.contains("  - cn.job.worker"));
        assertFalse(agentConfig.contains("  - com.cmb.music.app"));
        assertFalse(agentConfig.contains("  - com.cmb.music.common"));
    }

    @Test
    public void should_StopAtPackageDirectory_When_DirectoryContainsJavaAndSubPackage() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        writeJavaSource("src/main/java/com/cmb/music/MusicApplication.java", "com.cmb.music");
        writeJavaSource("src/main/java/com/cmb/music/app/OrderService.java", "com.cmb.music.app");
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertTrue(result.output.contains("已根据 src/main/java 目录推断 targetPackages=com.cmb.music"),
                result.output);
        assertTrue(agentConfig.contains("  - com.cmb.music"));
        assertFalse(agentConfig.contains("  - com.cmb.music.app"));
    }

    @Test
    public void should_InferEachModuleSourceRootIndependently_When_MavenMultiModuleProject() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        writeJavaSource("mall-admin/src/main/java/com/cmb/mall/admin/AdminApplication.java", "com.cmb.mall.admin");
        writeJavaSource("mall-admin/src/main/java/com/cmb/mall/admin/service/AdminService.java", "com.cmb.mall.admin.service");
        writeJavaSource("mall-app/src/main/java/com/cmb/mall/app/AppApplication.java", "com.cmb.mall.app");
        writeJavaSource("mall-common/src/main/java/com/cmb/mall/common/domain/Order.java", "com.cmb.mall.common.domain");
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertYamlListItem(agentConfig, "com.cmb.mall.admin");
        assertYamlListItem(agentConfig, "com.cmb.mall.app");
        assertYamlListItem(agentConfig, "com.cmb.mall.common.domain");
        assertNoYamlListItem(agentConfig, "com.cmb.mall");
    }

    @Test
    public void should_IgnoreDirectoryWithoutJava_When_CalculatingPackageBoundary() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        writeJavaSource("src/main/java/com/cmb/music/app/OrderService.java", "com.cmb.music.app");
        Files.createDirectories(tempDir.resolve("src/main/java/com/cmb/music/docs"));
        Files.write(tempDir.resolve("src/main/java/com/cmb/music/docs/readme.txt"),
                Arrays.asList("not java"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertYamlListItem(agentConfig, "com.cmb.music.app");
        assertNoYamlListItem(agentConfig, "com.cmb.music");
    }

    @Test
    public void should_IgnoreBuildOutputDirectories_When_TargetContainsJavaSources() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        writeJavaSource("src/main/java/com/cmb/music/app/OrderService.java", "com.cmb.music.app");
        writeJavaSource("target/generated-sources/src/main/java/cn/generated/BadSource.java", "cn.generated");
        writeJavaSource("build/generated/src/main/java/org/generated/BuildSource.java", "org.generated");
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertTrue(agentConfig.contains("  - com.cmb.music.app"));
        assertFalse(agentConfig.contains("  - cn.generated"));
        assertFalse(agentConfig.contains("  - org.generated"));
    }

    @Test
    public void should_UseServerConfiguredTargetPackages_When_NoSourceRootExists() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true, "\"com.company.configured\"");

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        String agentConfig = readUtf8(tempDir.resolve("target/codeperf/agent.yml"));
        assertTrue(result.output.contains("未能从目录结构推断 targetPackages，继续使用服务端配置: com.company.configured"),
                result.output);
        assertTrue(agentConfig.contains("  - com.company.configured"));
    }

    @Test
    public void should_Fail_When_TargetPackagesCannotBeDetermined() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true, "");

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(1, result.exitCode, result.output);
        assertTrue(result.output.contains("未能确定 targetPackages"), result.output);
    }

    @Test
    public void should_ResolveRemoteBranch_When_CiCheckoutDetachedHead() throws Exception {
        initGitRepository();
        run(tempDir, "git", "branch", "v1");
        run(tempDir, "git", "update-ref", "refs/remotes/origin/v1", "HEAD");
        run(tempDir, "git", "symbolic-ref", "refs/remotes/origin/HEAD", "refs/remotes/origin/v1");
        String commit = commandOutput(tempDir, "git", "rev-parse", "origin/v1^{commit}").trim();
        run(tempDir, "git", "checkout", "--detach", commit);
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        assertTrue(result.output.contains("branch=v1"), result.output);
        assertTrue(lastConfigRequestBody.contains("\"branch\":\"v1\""));
        assertTrue(readUtf8(tempDir.resolve("target/codeperf/build-info.properties")).contains("branch=v1"));
    }

    @Test
    public void should_SkipInjection_When_ServerConfigDisabled() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), false);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(0, result.exitCode, result.output);
        assertTrue(result.output.contains("服务端配置已禁用"));
        assertFalse(readUtf8(tempDir.resolve("Dockerfile")).contains("CODEPERF_AGENT_START"));
        assertFalse(Files.exists(tempDir.resolve("target/codeperf/codeperf-agent.jar")));
    }

    @Test
    public void should_Fail_When_DockerfileDoesNotExist() throws Exception {
        initGitRepository();
        String configUrl = startInstallConfigServer(sha256(AGENT_BYTES), true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(1, result.exitCode, result.output);
        assertTrue(result.output.contains("未找到 Dockerfile"));
        assertFalse(Files.exists(tempDir.resolve("target/codeperf/codeperf-agent.jar")));
    }

    @Test
    public void should_Fail_When_AgentChecksumMismatch() throws Exception {
        initGitRepository();
        Files.write(tempDir.resolve("Dockerfile"), Arrays.asList(
                "FROM eclipse-temurin:8-jre",
                "ENTRYPOINT [\"java\", \"-jar\", \"/app/app.jar\"]"), StandardCharsets.UTF_8);
        String configUrl = startInstallConfigServer("0000", true);

        ScriptResult result = runInstallScript(configUrl);

        assertEquals(1, result.exitCode, result.output);
        assertTrue(result.output.contains("SHA256 校验失败"));
    }

    private String startInstallConfigServer(String checksum, boolean enabled) throws IOException {
        return startInstallConfigServer(checksum, enabled, "\"com.demo.app\",\"com.demo.common\"");
    }

    private String startInstallConfigServer(String checksum, boolean enabled, String targetPackages) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/codeperf-agent.jar", this::handleAgentDownload);
        server.createContext("/api/agent/install-config",
                exchange -> handleInstallConfig(exchange, checksum, enabled, targetPackages));
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/agent/install-config";
    }

    private void handleAgentDownload(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, AGENT_BYTES.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(AGENT_BYTES);
        }
    }

    private void handleInstallConfig(HttpExchange exchange, String checksum, boolean enabled,
                                     String targetPackages) throws IOException {
        lastConfigRequestBody = readProcessOutput(exchange.getRequestBody()).replace("\n", "");
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        String response = "{"
                + "\"enabled\":" + enabled + ","
                + "\"serverUrl\":\"http://codeperf-server:9095\","
                + "\"agentUrl\":\"" + baseUrl + "/codeperf-agent.jar\","
                + "\"agentSha256\":\"" + checksum + "\","
                + "\"env\":\"dev\","
                + "\"targetPackages\":[" + targetPackages + "],"
                + "\"excludedPackages\":[\"com.cmb.cjtz\",\"com.cmb.checkerframework\",\"com.cmb.bee\",\"com.cmbchina.ugw\"],"
                + "\"entry\":{\"method\":\"POST\",\"path\":\"/api/orders/report\"},"
                + "\"slowSqlMs\":500,"
                + "\"sampleMs\":10,"
                + "\"connectTimeoutMs\":5000,"
                + "\"readTimeoutMs\":60000,"
                + "\"mode\":\"session\""
                + "}";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void initGitRepository() throws Exception {
        run(tempDir, "git", "init");
        run(tempDir, "git", "config", "user.name", "Developer");
        run(tempDir, "git", "config", "user.email", "developer@example.com");
        run(tempDir, "git", "remote", "add", "origin", "git@gitlab.example.com:demo/demo-app.git");
        Files.write(tempDir.resolve("README.md"), Arrays.asList("demo"), StandardCharsets.UTF_8);
        run(tempDir, "git", "add", "README.md");
        run(tempDir, "git", "commit", "-m", "initial commit");
    }

    private void writeJavaSource(String file, String packageName) throws IOException {
        Path javaFile = tempDir.resolve(file);
        Files.createDirectories(javaFile.getParent());
        Files.write(javaFile, Arrays.asList(
                "package " + packageName + ";",
                "public class " + javaFile.getFileName().toString().replace(".java", "") + " { }"),
                StandardCharsets.UTF_8);
    }

    private ScriptResult runInstallScript(String configUrl) throws Exception {
        Path script = Paths.get("..", "codeperf-server", "src", "main", "resources", "agent", "install.sh")
                .toAbsolutePath()
                .normalize();
        List<String> command = new ArrayList<>();
        command.add(findBash());
        command.add(script.toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(tempDir.toFile());
        builder.redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        env.put("CODEPERF_INSTALL_CONFIG_URL", configUrl);
        Process process = builder.start();
        String output = readProcessOutput(process.getInputStream());
        return new ScriptResult(process.waitFor(), output);
    }

    private void run(Path directory, String... command) throws Exception {
        ScriptResult result = commandResult(directory, command);
        assertEquals(0, result.exitCode, result.output);
    }

    private String commandOutput(Path directory, String... command) throws Exception {
        return commandResult(directory, command).output;
    }

    private ScriptResult commandResult(Path directory, String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(directory.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = readProcessOutput(process.getInputStream());
        return new ScriptResult(process.waitFor(), output);
    }

    private String findBash() {
        List<String> candidates = Arrays.asList(
                "C:\\Program Files\\Git\\bin\\bash.exe",
                "C:\\Program Files\\Git\\usr\\bin\\bash.exe",
                "bash");
        for (String candidate : candidates) {
            if ("bash".equals(candidate) || Files.isRegularFile(Paths.get(candidate))) {
                return candidate;
            }
        }
        return "bash";
    }

    private String readProcessOutput(InputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private String readUtf8(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private int count(String text, String pattern) {
        int total = 0;
        int index = text.indexOf(pattern);
        while (index >= 0) {
            total++;
            index = text.indexOf(pattern, index + pattern.length());
        }
        return total;
    }

    private void assertYamlListItem(String yaml, String value) {
        assertTrue(yaml.contains("\n  - " + value + "\n"), yaml);
    }

    private void assertNoYamlListItem(String yaml, String value) {
        assertFalse(yaml.contains("\n  - " + value + "\n"), yaml);
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static final class ScriptResult {
        private final int exitCode;
        private final String output;

        private ScriptResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}

