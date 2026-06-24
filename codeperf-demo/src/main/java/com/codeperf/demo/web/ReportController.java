package com.codeperf.demo.web;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import com.codeperf.demo.repo.UserRepository;
import com.codeperf.demo.service.OrderReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final OrderReportService reportService;
    private final UserRepository repo;

    public ReportController(OrderReportService reportService, UserRepository repo) {
        this.reportService = reportService;
        this.repo = repo;
    }

    /** 主坑接口：触发 N+1 / O(n^2) / CPU 热点 / 高分配。支持 POST 和 GET。 */
    @PostMapping("/orders/report")
    @GetMapping("/orders/report")
    public Map<String, Object> report() {
        System.out.println("[demo] >>> 收到请求 POST/GET /api/orders/report");
        return reportService.generateReport();
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
}
