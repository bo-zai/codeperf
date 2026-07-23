package com.cmb.codeperf.analysis.staticanalysis.rules;

import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRuleConfig;
import com.cmb.codeperf.analysis.staticanalysis.rule.StaticRuleContext;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoopIoAmplificationRuleTest {

    @Test
    public void should_ReportRepositoryCall_When_CallInsideLoop() throws Exception {
        List<StaticFinding> findings = analyze(RepositoryLoopFixture.class);

        assertEquals(1, findings.size());
        StaticFinding finding = findings.get(0);
        assertEquals("Loop I/O Amplification", finding.getType());
        assertEquals("DB", finding.getIoType());
        assertEquals("findById", finding.getCallName());
        assertTrue(finding.getLineNumber() > 0);
    }

    @Test
    public void should_ReportClientCall_When_CallInsideLoop() throws Exception {
        List<StaticFinding> findings = analyze(ClientLoopFixture.class);

        assertEquals(1, findings.size());
        assertEquals("HTTP", findings.get(0).getIoType());
        assertEquals("queryStock", findings.get(0).getCallName());
    }

    @Test
    public void should_IgnoreExternalCall_When_CallOutsideLoop() throws Exception {
        List<StaticFinding> findings = analyze(NonLoopFixture.class);

        assertEquals(0, findings.size());
    }

    private static List<StaticFinding> analyze(Class<?> type) throws Exception {
        ClassAnalysis analysis = com.cmb.codeperf.analysis.staticanalysis.BytecodeAnalyzer.analyze(readClassBytes(type));
        LoopIoAmplificationRule rule = new LoopIoAmplificationRule();
        return rule.analyze(new StaticRuleContext(
                Collections.singletonList(analysis),
                "com.cmb.codeperf.analysis.staticanalysis.rules",
                Collections.emptyList(),
                StaticRuleConfig.empty()));
    }

    private static byte[] readClassBytes(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing test class resource: " + resource);
            }
            byte[] buffer = new byte[8192];
            int read;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    interface UserRepository {
        String findById(String id);
    }

    interface InventoryClient {
        String queryStock(String id);
    }

    static class RepositoryLoopFixture {
        private final UserRepository repository = id -> id;

        void load(List<String> ids) {
            for (String id : ids) {
                repository.findById(id);
            }
        }
    }

    static class ClientLoopFixture {
        private final InventoryClient inventoryClient = id -> id;

        void load(String[] ids) {
            for (String id : ids) {
                inventoryClient.queryStock(id);
            }
        }
    }

    static class NonLoopFixture {
        private final InventoryClient inventoryClient = id -> id;

        void load() {
            inventoryClient.queryStock("one");
        }
    }
}

