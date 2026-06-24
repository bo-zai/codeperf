package com.codeperf.analysis.staticanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对单个类的方法级别的分析结果。由 BytecodeAnalyzer 产出，供各规则查询。
 */
public class ClassAnalysis {

    private final String className; // 内部名，如 com/demo/service/OrderReportService
    private final boolean feignClient; // 类本身是否标注 @FeignClient
    private final List<MethodAnalysis> methods = new ArrayList<>();

    public ClassAnalysis(String className, boolean feignClient) {
        this.className = className;
        this.feignClient = feignClient;
    }

    public void addMethod(MethodAnalysis m) { methods.add(m); }

    /** 二进制名（点分隔），如 com.demo.service.OrderReportService */
    public String getClassName() {
        return className.replace('/', '.');
    }

    public boolean isFeignClient() { return feignClient; }

    public List<MethodAnalysis> getMethods() { return methods; }

    // ---------- inner types ----------

    /** 方法级分析数据。 */
    public static class MethodAnalysis {
        private final String name;
        private final String descriptor;
        private final Set<String> annotations = new HashSet<>(); // L...; 格式
        private final List<int[]> loopRanges = new ArrayList<>();   // [startIdx, endIdx] 对
        private final List<CallSite> calls = new ArrayList<>();
        private final List<AllocSite> allocations = new ArrayList<>();
        private int mathCallCount;
        private int maxIntConst; // 方法内出现过的最大 int 常量（用于探测大循环边界）

        public MethodAnalysis(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        public String getName() { return name; }
        public String getDescriptor() { return descriptor; }
        public Set<String> getAnnotations() { return annotations; }
        public List<int[]> getLoopRanges() { return loopRanges; }
        public List<CallSite> getCalls() { return calls; }
        public List<AllocSite> getAllocations() { return allocations; }
        public int getMathCallCount() { return mathCallCount; }
        public int getMaxIntConst() { return maxIntConst; }

        public void addAnnotation(String desc) { annotations.add(desc); }
        public void addLoopRange(int start, int end) { loopRanges.add(new int[]{start, end}); }
        public void addCall(CallSite c) { calls.add(c); }
        public void addAllocation(AllocSite a) { allocations.add(a); }
        public void incMathCalls() { mathCallCount++; }
        public void noteIntConst(int v) { if (v > maxIntConst) maxIntConst = v; }

        /** 判定某指令是否在循环体内。 */
        public boolean inLoop(int insnIdx) {
            for (int[] r : loopRanges) {
                if (r[0] <= insnIdx && insnIdx <= r[1]) return true;
            }
            return false;
        }

        /** 判定某指令是否处在嵌套循环内（被 ≥2 个范围覆盖）。 */
        public boolean inNestedLoop(int insnIdx) {
            int cover = 0;
            for (int[] r : loopRanges) {
                if (r[0] <= insnIdx && insnIdx <= r[1]) cover++;
            }
            return cover >= 2;
        }
    }

    /** 一次方法调用指令。 */
    public static class CallSite {
        public final int insnIdx;
        public final String owner;   // 内部名
        public final String name;
        public final String desc;
        public final boolean isInterface;

        public CallSite(int insnIdx, String owner, String name, String desc, boolean isInterface) {
            this.insnIdx = insnIdx;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.isInterface = isInterface;
        }
    }

    /** 一次数组分配指令（NEWARRAY / ANEWARRAY）。size=-1 表示非常量。 */
    public static class AllocSite {
        public final int insnIdx;
        public final int size; // bytes

        public AllocSite(int insnIdx, int size) {
            this.insnIdx = insnIdx;
            this.size = size;
        }
    }
}
