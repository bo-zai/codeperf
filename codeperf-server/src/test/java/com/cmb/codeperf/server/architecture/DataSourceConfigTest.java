package com.cmb.codeperf.server.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据源配置约束测试。
 * 本地和测试验证常会同时启动多个 server 进程，连接池不设上限会放大 MySQL 连接数压力。
 */
public class DataSourceConfigTest {

    @Test
    public void should_LimitHikariPool_When_ServerRunsWithMysql() throws IOException {
        String yaml = new String(Files.readAllBytes(Paths.get("src/main/resources/application.yml")),
                StandardCharsets.UTF_8);

        assertTrue(yaml.contains("maximum-pool-size: ${CODEPERF_DB_MAX_POOL_SIZE:3}"));
        assertTrue(yaml.contains("minimum-idle: ${CODEPERF_DB_MIN_IDLE:0}"));
        assertTrue(yaml.contains("connection-timeout: ${CODEPERF_DB_CONNECTION_TIMEOUT_MS:5000}"));
    }
}
