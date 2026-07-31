package com.cmb.codeperf.demo.app.controller;

import com.cmb.codeperf.demo.app.service.OrderFulfillmentService;
import com.cmb.codeperf.demo.app.support.audit.BusinessAuditAspect;
import com.cmb.codeperf.demo.app.support.mybatis.SqlAuditInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单履约接口。
 * 接口模拟真实用户端页面查询：前端传入一批用户，后端汇总每个用户的订单履约信息。
 */
@RestController
@RequestMapping("/demo/orders/fulfillment")
public class OrderFulfillmentController {

    private final OrderFulfillmentService orderFulfillmentService;
    private final BusinessAuditAspect businessAuditAspect;
    private final SqlAuditInterceptor sqlAuditInterceptor;

    public OrderFulfillmentController(OrderFulfillmentService orderFulfillmentService,
                                      BusinessAuditAspect businessAuditAspect,
                                      SqlAuditInterceptor sqlAuditInterceptor) {
        this.orderFulfillmentService = orderFulfillmentService;
        this.businessAuditAspect = businessAuditAspect;
        this.sqlAuditInterceptor = sqlAuditInterceptor;
    }

    /**
     * 查询履约预览。
     *
     * @param userIds 用户 ID 列表，未传时使用固定演示数据
     * @return 履约预览结果
     */
    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam(value = "userIds", required = false) List<Long> userIds) {
        List<Long> effectiveUserIds = userIds == null || userIds.isEmpty()
                ? Arrays.asList(4101L, 4102L, 4103L, 4104L, 4105L)
                : userIds;
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userIds", effectiveUserIds);
        response.put("rows", orderFulfillmentService.buildFulfillmentRows(effectiveUserIds));
        response.put("auditCount", businessAuditAspect.getAuditCount());
        response.put("sqlAuditCount", sqlAuditInterceptor.getQueryCount());
        return response;
    }
}
