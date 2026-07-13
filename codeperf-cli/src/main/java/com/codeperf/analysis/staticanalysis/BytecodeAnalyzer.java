package com.codeperf.analysis.staticanalysis;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/**
 * ASM ClassVisitor 骨架：把单个 .class 的字节码解析为 {@link ClassAnalysis}。
 *
 * <p>收集内容：类名、是否 @FeignClient、方法注解、循环区间（基于回边检测）、
 * 方法调用指令、数组分配指令、Math 调用次数、方法内最大 int 常量。
 *
 * <p>见 docs/05-static-analysis.md 第 5 节。
 */
public final class BytecodeAnalyzer {

    private BytecodeAnalyzer() {}

    /** 解析一份 class 字节，产出 ClassAnalysis。 */
    public static ClassAnalysis analyze(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        Collector collector = new Collector();
        // Keep debug line numbers so findings can point back to source code.
        cr.accept(collector, ClassReader.SKIP_FRAMES);
        return collector.build();
    }

    // ---------------- ClassVisitor ----------------

    private static final class Collector extends ClassVisitor {
        private String className;
        private boolean feignClient;
        private final java.util.List<ClassAnalysis.MethodAnalysis> methods = new java.util.ArrayList<>();

        Collector() { super(Opcodes.ASM9); }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.className = name;
        }

        @Override
        public void visitSource(String source, String debug) {
            // Source paths are resolved later from source roots; keep class-level bytecode parsing focused.
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor != null && descriptor.contains("FeignClient")) {
                feignClient = true;
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            ClassAnalysis.MethodAnalysis ma = new ClassAnalysis.MethodAnalysis(name, descriptor);
            methods.add(ma);
            return new InsnCollector(ma);
        }

        ClassAnalysis build() {
            ClassAnalysis ca = new ClassAnalysis(className == null ? "" : className, feignClient);
            for (ClassAnalysis.MethodAnalysis m : methods) {
                ca.addMethod(m);
            }
            return ca;
        }
    }

    // ---------------- MethodVisitor ----------------

    private static final class InsnCollector extends MethodVisitor {
        private final ClassAnalysis.MethodAnalysis ma;
        private int insnIdx = 0;
        private int currentLine = 0;
        // label -> 它所指向的指令索引（visitLabel 时的当前 insnIdx）
        private final Map<Label, Integer> labelPos = new HashMap<>();
        // 紧邻当前指令之前压栈的 int 常量；null 表示上一条不是常量压栈
        private Integer pendingConst = null;

        InsnCollector(ClassAnalysis.MethodAnalysis ma) {
            super(Opcodes.ASM9);
            this.ma = ma;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            ma.addAnnotation(descriptor);
            return null;
        }

        @Override
        public void visitLabel(Label label) {
            // 记录 label 指向的指令索引（即下一条将被访问的指令）。
            labelPos.put(label, insnIdx);
            // visitLabel 不是真实指令，不推进 insnIdx，也不清除 pendingConst。
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            currentLine = line;
            ma.noteInstructionLine(insnIdx, line);
        }

        @Override
        public void visitInsn(int opcode) {
            Integer constVal = null;
            switch (opcode) {
                case Opcodes.ICONST_M1: constVal = -1; break;
                case Opcodes.ICONST_0:  constVal = 0;  break;
                case Opcodes.ICONST_1:  constVal = 1;  break;
                case Opcodes.ICONST_2:  constVal = 2;  break;
                case Opcodes.ICONST_3:  constVal = 3;  break;
                case Opcodes.ICONST_4:  constVal = 4;  break;
                case Opcodes.ICONST_5:  constVal = 5;  break;
                default: break;
            }
            if (constVal != null) {
                ma.noteIntConst(constVal);
            }
            advance(constVal);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.NEWARRAY) {
                // operand = 元素类型码；数组长度在栈顶（= pendingConst）。
                int elemBytes = primitiveArrayElemBytes(operand);
                int sizeBytes = (pendingConst != null && pendingConst >= 0)
                        ? pendingConst * elemBytes : -1;
                ma.addAllocation(new ClassAnalysis.AllocSite(insnIdx, sizeBytes, currentLine));
                advance(null);
            } else if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                ma.noteIntConst(operand);
                advance(operand);
            } else {
                advance(null);
            }
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.ANEWARRAY) {
                // 引用数组，按 8 字节/元素估算。
                int sizeBytes = (pendingConst != null && pendingConst >= 0)
                        ? pendingConst * 8 : -1;
                ma.addAllocation(new ClassAnalysis.AllocSite(insnIdx, sizeBytes, currentLine));
            }
            advance(null);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            // 多维数组：尺寸不易静态确定，记为变量尺寸。
            ma.addAllocation(new ClassAnalysis.AllocSite(insnIdx, -1, currentLine));
            advance(null);
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Integer) {
                int v = (Integer) value;
                ma.noteIntConst(v);
                advance(v);
            } else if (value instanceof Long) {
                long v = (Long) value;
                if (v <= Integer.MAX_VALUE && v >= Integer.MIN_VALUE) {
                    ma.noteIntConst((int) v);
                } else {
                    ma.noteIntConst(Integer.MAX_VALUE);
                }
                advance(null); // long 不能作为 newarray 长度直接来源
            } else {
                advance(null);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
            ma.addCall(new ClassAnalysis.CallSite(insnIdx, owner, name, descriptor, isInterface, currentLine));
            if ("java/lang/Math".equals(owner)) {
                ma.incMathCalls();
            }
            advance(null);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            Integer target = labelPos.get(label);
            if (target != null && target <= insnIdx) {
                // 回边：循环。区间 [循环起点, 回跳指令]。
                ma.addLoopRange(target, insnIdx, ma.lineForInstruction(target), currentLine);
            }
            advance(null);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) { advance(null); }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) { advance(null); }

        @Override
        public void visitIincInsn(int varIndex, int increment) { advance(null); }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor,
                                           org.objectweb.asm.Handle bsm, Object... args) {
            advance(null);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            checkBackEdges(dflt, labels);
            advance(null);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            checkBackEdges(dflt, labels);
            advance(null);
        }

        private void checkBackEdges(Label dflt, Label[] labels) {
            recordIfBack(dflt);
            if (labels != null) {
                for (Label l : labels) recordIfBack(l);
            }
        }

        private void recordIfBack(Label l) {
            Integer target = labelPos.get(l);
            if (target != null && target <= insnIdx) {
                ma.addLoopRange(target, insnIdx, ma.lineForInstruction(target), currentLine);
            }
        }

        /** 推进指令索引，并设置下一条指令可见的 pendingConst。 */
        private void advance(Integer newPending) {
            ma.noteInstructionLine(insnIdx, currentLine);
            insnIdx++;
            pendingConst = newPending;
        }

        private static int primitiveArrayElemBytes(int newarrayOperand) {
            switch (newarrayOperand) {
                case Opcodes.T_BOOLEAN: // 4
                case Opcodes.T_BYTE:    // 8
                    return 1;
                case Opcodes.T_CHAR:    // 5
                case Opcodes.T_SHORT:   // 9
                    return 2;
                case Opcodes.T_INT:     // 10
                case Opcodes.T_FLOAT:   // 6
                    return 4;
                case Opcodes.T_LONG:    // 11
                case Opcodes.T_DOUBLE:  // 7
                    return 8;
                default:
                    return 1;
            }
        }
    }
}
