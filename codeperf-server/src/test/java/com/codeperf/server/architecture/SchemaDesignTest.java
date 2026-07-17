package com.codeperf.server.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaDesignTest {

    private static final String[] ENTERPRISE_TABLES = {
            "code_repository",
            "codeperf_user",
            "git_commit",
            "analysis_task",
            "agent_session",
            "rule_definition",
            "static_finding",
            "dynamic_evidence",
            "finding_issue",
            "finding_occurrence",
            "scan_baseline",
            "risk_waiver",
            "notification_record"
    };

    private static final String[] REMOVED_TABLES = {
            "developer_identity",
            "repository_member",
            "static_result",
            "production_profile",
            "analysis_report"
    };

    @Test
    public void should_NotDeclareDatabaseForeignKeys_When_UsingLogicalReferences() throws IOException {
        String schema = normalizedSchema();

        assertFalse(schema.contains("foreign key"), "schema must not declare database foreign keys");
        assertFalse(schema.contains("references "), "schema must not declare database references");
    }

    @Test
    public void should_RequireLogicalReferenceColumns_When_StaticAndDynamicDataLinkedToTasks() throws IOException {
        String schema = normalizedSchema();

        assertTrue(schema.contains("repository_id bigint not null"), "analysis_task must require repository_id");
        assertTrue(schema.contains("git_commit_id bigint not null"), "analysis_task must require git_commit_id");
        assertTrue(schema.contains("create table if not exists static_finding"));
        assertTrue(schema.contains("create table if not exists dynamic_evidence"));
        assertTableColumn(schema, "static_finding", "task_id varchar(64) not null");
        assertTableColumn(schema, "dynamic_evidence", "task_id varchar(64) not null");
        assertTableColumn(schema, "dynamic_evidence", "agent_session_id bigint");
    }

    @Test
    public void should_DefineCompleteEnterpriseTables_When_SupportingMultiRepositoryGovernance() throws IOException {
        String schema = normalizedSchema();

        for (String table : ENTERPRISE_TABLES) {
            assertTrue(schema.contains("create table if not exists " + table), "missing table: " + table);
        }
        assertTableColumn(schema, "codeperf_user", "user_id varchar(128) not null");
        assertTableColumn(schema, "codeperf_user", "user_name varchar(128)");
        assertTableColumn(schema, "codeperf_user", "sap_id varchar(128)");
        assertTableColumn(schema, "codeperf_user", "email varchar(256) not null");
        assertTableColumn(schema, "git_commit", "author_time datetime");
        assertTableColumn(schema, "rule_definition", "rule_id varchar(128) not null");
        assertTableColumn(schema, "finding_issue", "issue_key varchar(128) not null");
        assertTableColumn(schema, "finding_occurrence", "issue_id bigint not null");
        assertTableColumn(schema, "agent_session", "session_id varchar(64) not null");
        assertTableColumn(schema, "scan_baseline", "baseline_key varchar(256) not null");
        assertTableColumn(schema, "risk_waiver", "issue_id bigint not null");
        assertTableColumn(schema, "notification_record", "recipient_user_id bigint");
        assertTableColumn(schema, "notification_record", "match_source varchar(64)");
        assertTableColumn(schema, "notification_record", "recipient_email varchar(256)");
    }

    @Test
    public void should_DefineChineseComments_When_CreatingEnterpriseTables() throws IOException {
        String schema = normalizedSchema();

        for (String table : ENTERPRISE_TABLES) {
            assertTableComment(schema, table);
        }
        assertTableColumn(schema, "code_repository", "repo_key varchar(512) not null comment");
        assertTableColumn(schema, "codeperf_user", "user_id varchar(128) not null comment");
        assertTableColumn(schema, "git_commit", "author_email varchar(256) comment");
        assertTableColumn(schema, "analysis_task", "task_id varchar(64) not null comment");
        assertTableColumn(schema, "notification_record", "match_source varchar(64) comment");
        assertTrue(schema.contains("'loop_io_amplification'"), "schema must initialize loop I/O rule definition");
    }

    @Test
    public void should_NotContainLegacyDemoTables_When_ServerUsesEnterpriseTables() throws IOException {
        String schema = normalizedSchema();

        for (String table : REMOVED_TABLES) {
            assertFalse(schema.contains("create table if not exists " + table),
                    "schema should not contain removed table: " + table);
        }
    }

    private String normalizedSchema() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (input == null) {
                throw new IOException("schema.sql not found");
            }
            return readStream(input)
                    .replace("\r\n", "\n")
                    .toLowerCase(Locale.ROOT);
        }
    }

    private String readStream(InputStream input) throws IOException {
        byte[] bytes = new byte[8192];
        StringBuilder content = new StringBuilder();
        int read;
        while ((read = input.read(bytes)) >= 0) {
            content.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
        }
        return content.toString();
    }

    private void assertTableColumn(String schema, String tableName, String expectedColumn) {
        int tableStart = schema.indexOf("create table if not exists " + tableName);
        assertTrue(tableStart >= 0, "missing table: " + tableName);
        int nextTable = schema.indexOf("create table if not exists ", tableStart + 1);
        String tableDefinition = nextTable >= 0 ? schema.substring(tableStart, nextTable) : schema.substring(tableStart);
        assertTrue(tableDefinition.contains(expectedColumn),
                tableName + " must declare column: " + expectedColumn);
    }

    private void assertTableComment(String schema, String tableName) {
        int tableStart = schema.indexOf("create table if not exists " + tableName);
        assertTrue(tableStart >= 0, "missing table: " + tableName);
        int nextTable = schema.indexOf("create table if not exists ", tableStart + 1);
        String tableDefinition = nextTable >= 0 ? schema.substring(tableStart, nextTable) : schema.substring(tableStart);
        assertTrue(tableDefinition.contains(") comment="),
                tableName + " must declare table comment");
    }
}
