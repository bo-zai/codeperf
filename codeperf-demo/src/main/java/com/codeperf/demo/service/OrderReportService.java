package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单报表服务——故意串联多种性能反模式，作为 CodePerf 的检测靶子。
 * 各反模式与检测规则的对应关系见 docs/01-demo-app.md 第 3、4 节。
 *
 * 规模常量集中在此，便于放大坑的可见度；改动后须同步更新 docs/01-demo-app.md。
 */
@Service
public class OrderReportService {

    /** 步骤 D：CPU 热点循环的迭代次数。 */
    private static final int HOT_LOOP_ITERATIONS = 40_000_000;
    /** 步骤 E：每次请求分配的临时缓冲个数与单块大小（制造高分配量，但随即可回收）。 */
    private static final int ALLOC_CHUNKS = 200;
    private static final int ALLOC_CHUNK_BYTES = 1024 * 1024; // 1MB * 200 ≈ 200MB 瞬时分配

    private final UserRepository repo;

    public OrderReportService(UserRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> generateReport() {
        long start = System.currentTimeMillis();

        // 步骤 A：查全部用户（1 次查询）
        List<User> users = repo.findAllUsers();

        // 步骤 B：N+1 —— 对每个用户单独查订单
        List<Order> allOrders = new ArrayList<>();
        for (User u : users) {
            allOrders.addAll(repo.findOrdersByUserId(u.getId()));
        }

        // 步骤 C：O(n^2) —— 用 List.contains 在循环中去重
        List<String> distinctItems = distinctItemNames(allOrders);

        // 步骤 D：CPU 热点 —— 重计算循环
        double score = computeScore(allOrders);

        // 步骤 E：高分配 + 大对象 —— 拼接报文
        String reportText = buildReportText(users, allOrders, distinctItems);

        long cost = System.currentTimeMillis() - start;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userCount", users.size());
        summary.put("orderCount", allOrders.size());
        summary.put("distinctItemCount", distinctItems.size());
        summary.put("score", score);
        summary.put("reportLength", reportText.length());
        summary.put("costMs", cost);
        return summary;
    }

    /** O(n^2)：每次 add 前都对整个列表做线性 contains 扫描。 */
    private List<String> distinctItemNames(List<Order> orders) {
        List<String> result = new ArrayList<>();
        for (Order o : orders) {
            String item = o.getItemName();
            if (!result.contains(item)) { // O(n) * n = O(n^2)
                result.add(item);
            }
        }
        return result;
    }

    /** CPU 热点：大量迭代 + 浮点运算，模拟“算分”。 */
    private double computeScore(List<Order> orders) {
        double acc = 0;
        double base = orders.isEmpty() ? 1 : orders.size();
        for (int i = 0; i < HOT_LOOP_ITERATIONS; i++) {
            acc += Math.sqrt(i * 1.0 + base) * Math.sin(i * 0.000001);
        }
        return acc;
    }

    /** 高分配 + 大对象：循环分配临时缓冲（随即可回收），并拼接较大文本。 */
    private String buildReportText(List<User> users, List<Order> orders, List<String> items) {
        // 制造高瞬时分配量（不持有引用，便于 GC，但累计分配字节数很高）
        long checksum = 0;
        for (int i = 0; i < ALLOC_CHUNKS; i++) {
            byte[] buf = new byte[ALLOC_CHUNK_BYTES];
            buf[i % ALLOC_CHUNK_BYTES] = (byte) i;
            checksum += buf.length;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("== Order Report ==\n");
        sb.append("users=").append(users.size())
                .append(", orders=").append(orders.size())
                .append(", distinctItems=").append(items.size())
                .append(", allocChecksum=").append(checksum).append('\n');
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getAmount() != null) {
                total = total.add(o.getAmount());
            }
        }
        sb.append("totalAmount=").append(total).append('\n');
        return sb.toString();
    }
}
