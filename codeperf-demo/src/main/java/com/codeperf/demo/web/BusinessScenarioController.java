package com.codeperf.demo.web;

import com.codeperf.demo.service.BatchOrderAuditService;
import com.codeperf.demo.service.CustomerNotificationService;
import com.codeperf.demo.service.ProductSnapshotService;
import com.codeperf.demo.service.ReconciliationService;
import com.codeperf.demo.service.SafePrefetchDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BusinessScenarioController {

    private final BatchOrderAuditService batchOrderAuditService;
    private final CustomerNotificationService customerNotificationService;
    private final ReconciliationService reconciliationService;
    private final ProductSnapshotService productSnapshotService;
    private final SafePrefetchDashboardService safePrefetchDashboardService;
    private final UserIdRequestParser userIdRequestParser;

    public BusinessScenarioController(BatchOrderAuditService batchOrderAuditService,
                                      CustomerNotificationService customerNotificationService,
                                      ReconciliationService reconciliationService,
                                      ProductSnapshotService productSnapshotService,
                                      SafePrefetchDashboardService safePrefetchDashboardService,
                                      UserIdRequestParser userIdRequestParser) {
        this.batchOrderAuditService = batchOrderAuditService;
        this.customerNotificationService = customerNotificationService;
        this.reconciliationService = reconciliationService;
        this.productSnapshotService = productSnapshotService;
        this.safePrefetchDashboardService = safePrefetchDashboardService;
        this.userIdRequestParser = userIdRequestParser;
    }

    @GetMapping("/orders/audit")
    public List<Map<String, Object>> audit(@RequestParam(value = "userIds", required = false) String userIds) {
        return batchOrderAuditService.audit(userIdRequestParser.parse(userIds));
    }

    @GetMapping("/notifications/preview")
    public List<Map<String, Object>> notificationPreview(
            @RequestParam(value = "userIds", required = false) String userIds) {
        return customerNotificationService.previewMessages(userIdRequestParser.parse(userIds));
    }

    @GetMapping("/orders/reconciliation")
    public List<Map<String, Object>> reconciliation(
            @RequestParam(value = "userIds", required = false) String userIds) {
        return reconciliationService.reconcile(userIdRequestParser.parse(userIds));
    }

    @GetMapping("/products/snapshots")
    public List<Map<String, Object>> productSnapshots(
            @RequestParam(value = "userIds", required = false) String userIds) {
        return productSnapshotService.buildSnapshots(userIdRequestParser.parse(userIds));
    }

    @GetMapping("/dashboard/safe-prefetch")
    public List<Map<String, Object>> safePrefetchDashboard(
            @RequestParam(value = "userIds", required = false) String userIds) {
        return safePrefetchDashboardService.buildDashboard(userIdRequestParser.parse(userIds));
    }
}
