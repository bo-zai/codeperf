package com.codeperf.analysis.staticanalysis;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对单个类的方法级别的分析结果。由 BytecodeAnalyzer 产出，供各规则查询。
 */
@Getter
public class ClassAnalysis {

    @Getter(AccessLevel.NONE)
    private final String className; // 内部名，如 com/demo/service/OrderReportService
    private final boolean feignClient; // 类本身是否标注 @FeignClient
    private final List<MethodAnalysis> methods = new ArrayList<>();
    @Setter
    private String sourceFile;

    public ClassAnalysis(String className, boolean feignClient) {
        this.className = className;
        this.feignClient = feignClient;
    }

    public void addMethod(MethodAnalysis m) { methods.add(m); }

    /** 二进制名（点分隔），如 com.demo.service.OrderReportService */
    public String getClassName() {
        return className.replace('/', '.');
    }

    // ---------- inner types ----------

    /** 方法级分析数据。 */
    @Getter
    public static class MethodAnalysis {
        private final String name;
        private final String descriptor;
        private final Set<String> annotations = new HashSet<>(); // L...; 格式
        private final List<int[]> loopRanges = new ArrayList<>();   // [startIdx, endIdx] 对
        private final List<LoopRange> loopLineRanges = new ArrayList<>();
        private final List<CallSite> calls = new ArrayList<>();
        private final List<AllocSite> allocations = new ArrayList<>();
        private final Map<Integer, Integer> instructionLines = new HashMap<>();
        private int mathCallCount;
        private int maxIntConst; // 方法内出现过的最大 int 常量（用于探测大循环边界）

        public MethodAnalysis(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        public void addAnnotation(String desc) { annotations.add(desc); }
        public void addLoopRange(int start, int end) { loopRanges.add(new int[]{start, end}); }
        public void addLoopRange(int start, int end, int startLine, int endLine) {
            loopRanges.add(new int[]{start, end});
            loopLineRanges.add(new LoopRange(start, end, startLine, endLine));
        }
        public void addCall(CallSite c) { calls.add(c); }
        public void addAllocation(AllocSite a) { allocations.add(a); }
        public void incMathCalls() { mathCallCount++; }
        public void noteIntConst(int v) {
            if (v > maxIntConst) {
                maxIntConst = v;
            }
        }
        public void noteInstructionLine(int insnIdx, int line) {
            if (line > 0) {
                instructionLines.put(insnIdx, line);
            }
        }
        public int lineForInstruction(int insnIdx) {
            Integer exact = instructionLines.get(insnIdx);
            if (exact != null) {
                return exact;
            }
            int bestIdx = -1;
            int bestLine = 0;
            for (Map.Entry<Integer, Integer> entry : instructionLines.entrySet()) {
                int idx = entry.getKey();
                if (idx <= insnIdx && idx > bestIdx) {
                    bestIdx = idx;
                    bestLine = entry.getValue();
                }
            }
            return bestLine;
        }

        /** 判定某指令是否在循环体内。 */
        public boolean inLoop(int insnIdx) {
            for (int[] r : loopRanges) {
                if (r[0] <= insnIdx && insnIdx <= r[1]) {
                    return true;
                }
            }
            return false;
        }

        /** 判定某指令是否处在嵌套循环内（被 ≥2 个范围覆盖）。 */
        public boolean inNestedLoop(int insnIdx) {
            int cover = 0;
            for (int[] r : loopRanges) {
                if (r[0] <= insnIdx && insnIdx <= r[1]) {
                    cover++;
                }
            }
            return cover >= 2;
        }
    }

    /** 循环区间，包含字节码指令范围与源码行范围。 */
    @Getter
    @AllArgsConstructor
    public static class LoopRange {
        private final int startInsn;
        private final int endInsn;
        private final int startLine;
        private final int endLine;
    }

    /** 一次方法调用指令。 */
    @Getter
    public static class CallSite {
        private final int insnIdx;
        private final String owner;   // 内部名
        private final String name;
        private final String desc;
        private final boolean isInterface;
        private final int lineNumber;

        public CallSite(int insnIdx, String owner, String name, String desc, boolean isInterface) {
            this(insnIdx, owner, name, desc, isInterface, 0);
        }

        public CallSite(int insnIdx, String owner, String name, String desc, boolean isInterface, int lineNumber) {
            this.insnIdx = insnIdx;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.isInterface = isInterface;
            this.lineNumber = lineNumber;
        }
    }

    /** 一次数组分配指令（NEWARRAY / ANEWARRAY）。size=-1 表示非常量。 */
    @Getter
    public static class AllocSite {
        private final int insnIdx;
        private final int size; // bytes
        private final int lineNumber;

        public AllocSite(int insnIdx, int size) {
            this(insnIdx, size, 0);
        }

        public AllocSite(int insnIdx, int size, int lineNumber) {
            this.insnIdx = insnIdx;
            this.size = size;
            this.lineNumber = lineNumber;
        }
    }
}
