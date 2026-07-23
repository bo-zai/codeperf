package com.cmb.codeperf.analysis.staticanalysis.rules;

import com.cmb.codeperf.analysis.Severity;
import com.cmb.codeperf.analysis.staticanalysis.BytecodeRule;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis.MethodAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding.Confidence;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则 3.3：计算密集型方法。
 *
 * <p>方法体内 Math.* 调用 ≥ 5 次，或出现大常量循环边界（int 常量 > 1,000,000）→ MEDIUM。
 * 见 docs/05-static-analysis.md 第 3.3 节。
 */
public class HeavyComputeSuspect implements BytecodeRule {

    private static final String TYPE = "计算密集（CPU 热点嫌疑）";
    private static final int MATH_THRESHOLD = 5;
    private static final int BIG_CONST_THRESHOLD = 1_000_000;

    @Override
    public List<StaticFinding> analyze(List<ClassAnalysis> classes, String targetPackage) {
        List<StaticFinding> findings = new ArrayList<>();
        for (ClassAnalysis cls : classes) {
            for (MethodAnalysis m : cls.getMethods()) {
                boolean mathHeavy = m.getMathCallCount() >= MATH_THRESHOLD;
                boolean bigLoop = m.getMaxIntConst() > BIG_CONST_THRESHOLD;
                if (!mathHeavy && !bigLoop) continue;

                String classMethod = cls.getClassName() + "." + m.getName();
                StringBuilder ev = new StringBuilder();
                if (mathHeavy) {
                    ev.append("Math.* 调用 ").append(m.getMathCallCount()).append(" 次");
                }
                if (bigLoop) {
                    if (ev.length() > 0) ev.append("；");
                    ev.append("出现大常量 ").append(m.getMaxIntConst()).append("（疑似大循环边界）");
                }
                findings.add(new StaticFinding(
                        TYPE, Severity.WARN, Confidence.MEDIUM,
                        "方法包含密集数学运算或大循环，若被入口路径调用可能成为 CPU 热点。",
                        ev.toString(), classMethod));
            }
        }
        return findings;
    }
}

