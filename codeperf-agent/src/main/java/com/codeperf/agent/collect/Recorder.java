package com.codeperf.agent.collect;

import com.codeperf.agent.config.AgentConfig;
import com.codeperf.agent.session.CallNode;
import com.codeperf.agent.session.RequestData;
import com.codeperf.agent.session.SessionData;
import com.codeperf.agent.upload.DynamicEvidenceReporter;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 全局运行时记录器：所有 Advice 内联代码都委托到这里的静态方法。
 * 设计与归并策略见 docs/02-agent-core.md 第 4、5 节。
 *
 * 关键不变量：
 *  - 仅当「当前线程存在活跃请求」时，方法/SQL 才被记录（降噪 + 降开销）。
 *  - 所有方法对异常零容忍：内部 try/catch 吞掉，绝不影响目标业务。
 *  - MVP 默认 mode=session：只采集第一个匹配入口的请求，采到即完成并落盘。
 */
public final class Recorder {

    /** 方法未被记录的哨��值（enterMethod 返回，exitMethod 据此短路）。 */
    public static final long NOT_RECORDED = Long.MIN_VALUE;

    private static volatile AgentConfig config;
    private static volatile SessionData session;
    private static volatile Profiler sampler;
    private static volatile SessionWriter writer;
    private static volatile DynamicEvidenceReporter reporter;
    private static final AtomicBoolean completed = new AtomicBoolean(false);

    /** 当前线程正在测量的请求；为 null 表示本线程不在采集窗口内。 */
    private static final ThreadLocal<RequestData> ACTIVE = new ThreadLocal<>();
    /** 当前线程的方法调用栈帧。 */
    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    /** 入口请求的起始 nano（用于算 wallTime）。 */
    private static final ThreadLocal<Long> REQ_START_NANO = new ThreadLocal<>();

    /** PreparedStatement -> 其 SQL 文本的绑定（仅在活跃请求期间登记，避免泄漏）。 */
    private static final Map<Object, String> PREPARED_SQL =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private Recorder() {
    }

    public static void init(AgentConfig cfg, Profiler s, SessionWriter w) {
        init(cfg, s, w, null);
    }

    public static void init(AgentConfig cfg, Profiler s, SessionWriter w, DynamicEvidenceReporter r) {
        config = cfg;
        sampler = s;
        writer = w;
        reporter = r;
        SessionData sd = new SessionData();
        sd.setEntryMethod(cfg.getEntryMethod());
        sd.setEntryPath(cfg.getEntryPath());
        sd.setTargetPackages(cfg.getTargetPackages());
        sd.setStartTimeEpochMs(System.currentTimeMillis());
        sd.setJavaVersion(System.getProperty("java.version"));
        session = sd;
    }

    public static AgentConfig config() {
        return config;
    }

    public static boolean isCompleted() {
        return completed.get();
    }

    // ===================== 入口请求边界 =====================

    /**
     * 由 DispatcherServlet 的 Advice 调用：反射读取 HttpServletRequest 的 method/URI，
     * 与配置的 entry 匹配；匹配则开启采集窗口。不直接依赖 servlet 类型（反射）。
     * 匹配规则：method 忽略大小写相等，且 URI 以 entryPath 为前缀。
     * @return true 表示已开启（调用方需在退出时调用 finishRequest）。
     */
    public static boolean tryStartRequest(Object httpServletRequest) {
        try {
            if (httpServletRequest == null || completed.get()) {
                return false;
            }
            Class<?> c = httpServletRequest.getClass();
            String method = (String) c.getMethod("getMethod").invoke(httpServletRequest);
            String uri = (String) c.getMethod("getRequestURI").invoke(httpServletRequest);
            if (method == null || uri == null) {
                return false;
            }
            if (!method.equalsIgnoreCase(config.getEntryMethod())) {
                return false;
            }
            if (!uri.startsWith(config.getEntryPath())) {
                return false;
            }
            return startRequest(method.toUpperCase(), uri);
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * 尝试在当前线程开启一个请求采集窗口。
     * @return true 表示已开启（调用方需在退出时调用 finishRequest）。
     */
    public static boolean startRequest(String httpMethod, String path) {
        try {
            if (completed.get()) {
                return false;
            }
            if (ACTIVE.get() != null) {
                return false; // 同线程重入保护
            }
            RequestData rd = new RequestData();
            Thread t = Thread.currentThread();
            rd.setHttpMethod(httpMethod);
            rd.setPath(path);
            rd.setThreadName(t.getName());
            rd.setThreadId(t.getId());
            rd.setAllocBytes(currentThreadAllocatedBytes(t.getId())); // 暂存基线，finish 时算差
            ACTIVE.set(rd);
            STACK.get().clear();
            REQ_START_NANO.set(System.nanoTime());
            Profiler s = sampler;
            if (s != null) {
                s.setTarget(t, rd);
            }
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static void finishRequest() {
        try {
            RequestData rd = ACTIVE.get();
            if (rd == null) {
                return;
            }
            Long startNano = REQ_START_NANO.get();
            long wallMs = startNano == null ? 0 : (System.nanoTime() - startNano) / 1_000_000L;
            rd.setWallTimeMs(wallMs);
            long endAlloc = currentThreadAllocatedBytes(rd.getThreadId());
            long delta = endAlloc - rd.getAllocBytes();
            rd.setAllocBytes(delta < 0 ? 0 : delta);
            rd.setStatus(200);

            Profiler s = sampler;
            if (s != null) {
                s.clearTarget();
            }

            boolean firstComplete = false;
            if ("session".equals(config.getMode())) {
                if (completed.compareAndSet(false, true)) {
                    session.addRequest(rd);
                    firstComplete = true;
                }
            } else {
                synchronized (session) {
                    session.addRequest(rd);
                }
            }

            // 清理线程态
            ACTIVE.remove();
            STACK.get().clear();
            STACK.remove();
            REQ_START_NANO.remove();
            // 清理本请求登记的 prepared 绑定
            PREPARED_SQL.clear();

            if (firstComplete && writer != null) {
                writer.write(session); // 写数据 + .done 标记
            }
            if (firstComplete && reporter != null) {
                reporter.report(session);
            }
        } catch (Throwable ignore) {
            // 绝不影响业务
        }
    }

    // ===================== 方法插桩 =====================

    public static long enterMethod(String methodSig) {
        try {
            RequestData rd = ACTIVE.get();
            if (rd == null) {
                return NOT_RECORDED;
            }
            Deque<Frame> stack = STACK.get();
            CallNode parent = stack.isEmpty() ? rd.getCallTree() : stack.peek().node;
            CallNode node = parent.childFor(methodSig);
            stack.push(new Frame(node, System.nanoTime()));
            return 1L; // 仅作"已记录"标记；真实时间存于 Frame
        } catch (Throwable ignore) {
            return NOT_RECORDED;
        }
    }

    public static void exitMethod(long token) {
        if (token == NOT_RECORDED) {
            return;
        }
        try {
            Deque<Frame> stack = STACK.get();
            if (stack.isEmpty()) {
                return;
            }
            Frame f = stack.pop();
            long totalMs = (System.nanoTime() - f.startNano) / 1_000_000L;
            f.node.accumulate(totalMs, f.childMs);
            if (!stack.isEmpty()) {
                stack.peek().childMs += totalMs;
            }
        } catch (Throwable ignore) {
        }
    }

    // ===================== JDBC =====================

    /** 在活跃请求期间登记 PreparedStatement 与其 SQL 的绑定。 */
    public static void bindPrepared(Object ps, String sql) {
        try {
            if (ps == null || sql == null) {
                return;
            }
            if (ACTIVE.get() == null) {
                return; // 仅采集窗口内登记，避免全局泄漏
            }
            PREPARED_SQL.put(ps, sql);
        } catch (Throwable ignore) {
        }
    }

    /** PreparedStatement 无参 execute* 的记录：SQL 来自之前的绑定。 */
    public static void recordPreparedExecution(Object ps, long startNano) {
        try {
            String sql = PREPARED_SQL.get(ps);
            recordSql(sql, startNano);
        } catch (Throwable ignore) {
        }
    }

    /** Statement 直接传 SQL 的 execute* 记录。 */
    public static void recordStatementExecution(String sql, long startNano) {
        recordSql(sql, startNano);
    }

    private static void recordSql(String sql, long startNano) {
        try {
            RequestData rd = ACTIVE.get();
            if (rd == null || sql == null) {
                return;
            }
            long ms = (System.nanoTime() - startNano) / 1_000_000L;
            boolean slow = ms > config.getSlowSqlMs();
            String fp = SqlFingerprint.of(sql);
            rd.sqlRecord(fp, sql).addExecution(ms, slow);
        } catch (Throwable ignore) {
        }
    }

    // ===================== 辅助 =====================

    private static long currentThreadAllocatedBytes(long threadId) {
        try {
            java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            if (bean instanceof com.sun.management.ThreadMXBean) {
                com.sun.management.ThreadMXBean sun = (com.sun.management.ThreadMXBean) bean;
                if (sun.isThreadAllocatedMemorySupported()) {
                    return sun.getThreadAllocatedBytes(threadId);
                }
            }
        } catch (Throwable ignore) {
        }
        return 0L;
    }

    /** 调用栈帧。 */
    private static final class Frame {
        final CallNode node;
        final long startNano;
        long childMs;

        Frame(CallNode node, long startNano) {
            this.node = node;
            this.startNano = startNano;
        }
    }
}
