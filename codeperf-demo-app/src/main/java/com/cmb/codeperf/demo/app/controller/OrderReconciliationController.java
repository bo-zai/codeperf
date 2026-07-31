package com.cmb.codeperf.demo.app.controller;

import com.cmb.codeperf.demo.app.service.OrderReconciliationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单对账接口。
 * 该接口模拟真实业务中的批量对账页面：前端传入一批用户，后端逐个补齐订单与画像信息。
 */
@RestController
@RequestMapping("/demo/orders/reconciliation")
public class OrderReconciliationController {

    private final OrderReconciliationService orderReconciliationService;

    public OrderReconciliationController(OrderReconciliationService orderReconciliationService) {
        this.orderReconciliationService = orderReconciliationService;
    }

    /**
     * 生成订单对账数据。
     *
     * @param userIds 用户 ID 列表，未传时使用固定演示数据
     * @return 对账明细
     */
    @GetMapping("/rows")
    public Map<String, Object> rows(@RequestParam(value = "userIds", required = false) List<Long> userIds) {
        List<Long> effectiveUserIds = userIds == null || userIds.isEmpty()
                ? Arrays.asList(3101L, 3102L, 3103L, 3104L, 3105L)
                : userIds;
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userIds", effectiveUserIds);
        response.put("rows", orderReconciliationService.buildReconciliationRows(effectiveUserIds));
        return response;
    }
}
