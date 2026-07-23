package com.cmb.codeperf.analysis.staticanalysis.rules;

import com.cmb.codeperf.analysis.Severity;
import com.cmb.codeperf.analysis.staticanalysis.BytecodeRule;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis.CallSite;
import com.cmb.codeperf.analysis.staticanalysis.ClassAnalysis.MethodAnalysis;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding;
import com.cmb.codeperf.analysis.staticanalysis.StaticFinding.Confidence;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则 3.2：循环内 List.contains() 或嵌套循环 → O(n²)。
 *
 * <p>循环内 List/ArrayList.contains → HIGH；两层嵌套循环 → MEDIUM。
 * 见 docs/05-static-analysis.md 第 3.2 节。
 */
public class NSquaredSuspect implements BytecodeRule {

    private static final String TYPE = "O(n²) 嫌疑（嵌套循环 / 循环内 contains）";

    @Override
    public List<StaticFinding> analyze(List<ClassAnalysis> classes, String targetPackage) {
        List<StaticFinding> findings = new ArrayList<>();
        for (ClassAnalysis cls : classes) {
            for (MethodAnalysis m : cls.getMethods()) {
                if (m.getLoopRanges().isEmpty()) {
                    continue;
                }
                String classMethod = cls.getClassName() + "." + m.getName();

                // 信号 1：循环内 contains 调用（HIGH）
                CallSite containsCall = findContainsInLoop(m);
                if (containsCall != null) {
                    String evidence = "循环内调用 " + containsCall.getOwner().replace('/', '.')
                            + "#contains()（指令 #" + containsCall.getInsnIdx()
                            + "），线性查找嵌套在循环中 → O(n²)。";
                    findings.add(new StaticFinding(
                            TYPE, Severity.WARN, Confidence.HIGH,
                            "循环体内对 List 做 contains 线性查找，整体复杂度退化为 O(n²)。建议改用 Set/Map。",
                            evidence, classMethod));
                    continue;
                }

                // 信号 2：嵌套循环（MEDIUM）
                if (hasNestedLoop(m)) {
                    findings.add(new StaticFinding(
                            TYPE, Severity.WARN, Confidence.MEDIUM,
                            "方法存在两层及以上嵌套循环，可能是 O(n²) 计算或匹配。",
                            "检测到嵌套循环区间（内层循环位于外层循环体内）。",
                            classMethod));
                }
            }
        }
        return findings;
    }

    private CallSite findContainsInLoop(MethodAnalysis m) {
        for (CallSite call : m.getCalls()) {
            if (!"contains".equals(call.getName())) {
                continue;
            }
            if (!call.getOwner().contains("List")) {
                continue;
            }
            if (m.inLoop(call.getInsnIdx())) {
                return call;
            }
        }
        return null;
    }

    /** 是否存在一个循环区间被另一个循环区间真包含（即嵌套）。 */
    private boolean hasNestedLoop(MethodAnalysis m) {
        List<int[]> ranges = m.getLoopRanges();
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = 0; j < ranges.size(); j++) {
                if (i == j) {
                    continue;
                }
                int[] outer = ranges.get(i);
                int[] inner = ranges.get(j);
                boolean contained = outer[0] <= inner[0] && inner[1] <= outer[1];
                boolean strict = outer[0] != inner[0] || outer[1] != inner[1];
                if (contained && strict) {
                    return true;
                }
            }
        }
        return false;
    }
}

