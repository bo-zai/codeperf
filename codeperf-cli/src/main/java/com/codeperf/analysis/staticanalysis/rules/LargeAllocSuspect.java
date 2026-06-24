package com.codeperf.analysis.staticanalysis.rules;

import com.codeperf.analysis.Severity;
import com.codeperf.analysis.staticanalysis.BytecodeRule;
import com.codeperf.analysis.staticanalysis.ClassAnalysis;
import com.codeperf.analysis.staticanalysis.ClassAnalysis.AllocSite;
import com.codeperf.analysis.staticanalysis.ClassAnalysis.MethodAnalysis;
import com.codeperf.analysis.staticanalysis.StaticFinding;
import com.codeperf.analysis.staticanalysis.StaticFinding.Confidence;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则 3.4：大数组分配。
 *
 * <p>循环体内 NEWARRAY/ANEWARRAY（字节数 ≥ 1024 或尺寸为变量）→ MEDIUM。
 * <p>非循环中的大数组分配（≥ 1MB）→ LOW。
 * 见 docs/05-static-analysis.md 第 3.4 节。
 */
public class LargeAllocSuspect implements BytecodeRule {

    private static final String TYPE = "大数组分配";
    private static final int LOOP_BYTES_THRESHOLD = 1024;
    private static final int BIG_BYTES_THRESHOLD = 1024 * 1024; // 1MB

    @Override
    public List<StaticFinding> analyze(List<ClassAnalysis> classes, String targetPackage) {
        List<StaticFinding> findings = new ArrayList<>();
        for (ClassAnalysis cls : classes) {
            for (MethodAnalysis m : cls.getMethods()) {
                for (AllocSite a : m.getAllocations()) {
                    boolean inLoop = m.inLoop(a.insnIdx);
                    String classMethod = cls.getClassName() + "." + m.getName();
                    String sizeStr = a.size < 0 ? "变量尺寸" : (a.size + " 字节");

                    if (inLoop && (a.size < 0 || a.size >= LOOP_BYTES_THRESHOLD)) {
                        findings.add(new StaticFinding(
                                TYPE, Severity.WARN, Confidence.MEDIUM,
                                "循环体内分配数组，可能造成高频内存分配与 GC 压力。",
                                "循环内数组分配（" + sizeStr + "，指令 #" + a.insnIdx + "）。",
                                classMethod));
                    } else if (!inLoop && a.size >= BIG_BYTES_THRESHOLD) {
                        findings.add(new StaticFinding(
                                TYPE, Severity.INFO, Confidence.LOW,
                                "方法中存在大数组分配（≥ 1MB），可能带来内存峰值。",
                                "大数组分配（" + sizeStr + "，指令 #" + a.insnIdx + "）。",
                                classMethod));
                    }
                }
            }
        }
        return findings;
    }
}
