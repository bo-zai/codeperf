package com.codeperf.analysis.source.rule;

import com.codeperf.analysis.source.SourceFinding;
import com.codeperf.analysis.source.index.SourceClassIndex;
import com.codeperf.cli.config.StaticScanConfig;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoopIoAmplificationAstRuleTest {

    @Test
    public void should_ReportMapperCall_When_DirectlyCalledInsideForEachLoop() {
        CompilationUnit unit = StaticJavaParser.parse(
                "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      orderMapper.selectById(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        SourceClassIndex index = new SourceClassIndex();
        index.add(Paths.get("src/main/java/OrderService.java"), unit);
        SourceRuleContext context = new SourceRuleContext(
                Arrays.asList(unit),
                Paths.get("src/main/java/OrderService.java"),
                index,
                new StaticScanConfig());

        List<SourceFinding> findings = new LoopIoAmplificationAstRule().analyze(context);

        assertEquals(1, findings.size());
        assertEquals("DB", findings.get(0).getIoType());
        assertEquals(5, findings.get(0).getLineNumber());
        assertEquals(4, findings.get(0).getLoopStartLine());
    }

    @Test
    public void should_ReportIndirectMapperCall_When_LoopCallsSameClassMethod() {
        CompilationUnit unit = StaticJavaParser.parse(
                "class OrderService {\n"
                        + "  private OrderMapper orderMapper;\n"
                        + "  void buildReport(java.util.List<Long> ids) {\n"
                        + "    for (Long id : ids) {\n"
                        + "      loadOrder(id);\n"
                        + "    }\n"
                        + "  }\n"
                        + "  Order loadOrder(Long id) {\n"
                        + "    return orderMapper.selectById(id);\n"
                        + "  }\n"
                        + "}\n");
        SourceClassIndex index = new SourceClassIndex();
        index.add(Paths.get("src/main/java/OrderService.java"), unit);
        StaticScanConfig config = new StaticScanConfig();
        config.getCallChain().setEnabled(true);
        config.getCallChain().setMaxDepth(2);
        SourceRuleContext context = new SourceRuleContext(
                Arrays.asList(unit),
                Paths.get("src/main/java/OrderService.java"),
                index,
                config);

        List<SourceFinding> findings = new LoopIoAmplificationAstRule().analyze(context);

        assertEquals(1, findings.size());
        assertEquals("DB", findings.get(0).getIoType());
        assertEquals(2, findings.get(0).getCallChain().size());
        assertEquals("buildReport", findings.get(0).getCallChain().get(0).getMethodName());
        assertEquals("loadOrder", findings.get(0).getCallChain().get(1).getMethodName());
    }
}
