package com.cmb.codeperf.demo.app.controller;

import com.cmb.codeperf.demo.app.service.BulkOrderRiskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量订单风险接口。
 * <p>
 * 用于模拟业务里的批量查询和导出场景，便于 CodePerf 在真实项目代码上验证循环内 I/O 风险。
 */
@RestController
@RequestMapping("/demo/orders/risk")
public class BulkOrderRiskController {

    private final BulkOrderRiskService bulkOrderRiskService;

    public BulkOrderRiskController(BulkOrderRiskService bulkOrderRiskService) {
        this.bulkOrderRiskService = bulkOrderRiskService;
    }

    /**
     * 查询批量订单风险概览。
     *
     * @param userIds 用户 ID 列表，缺省时使用固定演示数据
     * @return 风险概览
     */
    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(value = "userIds", required = false) List<Long> userIds) {
        List<Long> effectiveUserIds = userIds == null || userIds.isEmpty()
                ? Arrays.asList(2001L, 2002L, 2003L, 2004L)
                : userIds;
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userIds", effectiveUserIds);
        response.put("rows", bulkOrderRiskService.buildRiskOverview(effectiveUserIds));
        return response;
    }
}
