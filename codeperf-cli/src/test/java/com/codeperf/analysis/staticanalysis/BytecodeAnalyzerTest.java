package com.codeperf.analysis.staticanalysis;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytecodeAnalyzerTest {

    @Test
    public void should_CaptureLineNumber_When_LoopBodyContainsCall() throws Exception {
        byte[] bytes = readClassBytes(BytecodeLineFixture.class);
        ClassAnalysis analysis = BytecodeAnalyzer.analyze(bytes);

        int line = -1;
        for (ClassAnalysis.MethodAnalysis method : analysis.getMethods()) {
            if (!"runLoop".equals(method.getName())) {
                continue;
            }
            for (ClassAnalysis.CallSite call : method.getCalls()) {
                if ("touch".equals(call.getName())) {
                    line = call.getLineNumber();
                }
            }
        }

        assertTrue(line > 0, "expected call line number to be captured");
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

    static class BytecodeLineFixture {
        void runLoop(java.util.List<String> values) {
            for (String value : values) {
                touch(value);
            }
        }

        void touch(String value) {
            if (value == null) {
                throw new IllegalArgumentException();
            }
        }
    }
}
