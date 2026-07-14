package com.codeperf.demo.web;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import com.codeperf.demo.repo.UserRepository;
import com.codeperf.demo.service.OrderExportService;
import com.codeperf.demo.service.OrderReportService;
import com.codeperf.demo.service.OrderSettlementService;
import com.codeperf.demo.service.SafeBatchOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final OrderReportService reportService;
    private final OrderExportService exportService;
    private final OrderSettlementService settlementService;
    private final SafeBatchOrderService safeBatchOrderService;
    private final UserRepository repo;

    public ReportController(OrderReportService reportService,
                            OrderExportService exportService,
                            OrderSettlementService settlementService,
                            SafeBatchOrderService safeBatchOrderService,
                            UserRepository repo) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.settlementService = settlementService;
        this.safeBatchOrderService = safeBatchOrderService;
        this.repo = repo;
    }

    /** 主坑接口：触发 N+1 / O(n^2) / CPU 热点 / 高分配。支持 POST 和 GET。 */
    @PostMapping("/orders/report")
    @GetMapping("/orders/report")
    public Map<String, Object> report() {
        System.out.println("[demo] >>> 收到请求 POST/GET /api/orders/report");
        return reportService.generateReport();
    }

    @GetMapping("/orders/export")
    public List<Map<String, Object>> export(@RequestParam(value = "userIds", required = false) String userIds) {
        return exportService.exportRows(resolveUserIds(userIds));
    }

    @PostMapping("/orders/settlement")
    @GetMapping("/orders/settlement")
    public Map<String, Object> settlement(@RequestParam(value = "userIds", required = false) String userIds) {
        return settlementService.settle(resolveUserIds(userIds));
    }

    @GetMapping("/orders/safe-batch")
    public List<Map<String, Object>> safeBatch(@RequestParam(value = "userIds", required = false) String userIds) {
        return safeBatchOrderService.buildRows(resolveUserIds(userIds));
    }

    /** 基线接口：轻量，干净，验证工具不误报。 */
    @GetMapping("/users/{id}")
    public Map<String, Object> user(@PathVariable("id") long id) {
        User u = repo.findUserById(id);
        List<Order> orders = repo.findOrdersByUserId(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", u);
        result.put("orderCount", orders.size());
        return result;
    }

    private List<Long> resolveUserIds(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Arrays.asList(1L, 2L, 3L, 4L, 5L);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
