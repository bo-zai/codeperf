package com.cmb.codeperf.demo.app.controller;

import com.cmb.codeperf.demo.app.service.ManualBatchExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量导出接口。
 * <p>
 * 用于模拟业务中“导出用户 + 订单 + 物流补齐”的常见场景，方便在本地手动运行 CodePerf scan 验证。
 */
@RestController
@RequestMapping("/demo/export")
public class ManualBatchExportController {

    private final ManualBatchExportService manualBatchExportService;

    public ManualBatchExportController(ManualBatchExportService manualBatchExportService) {
        this.manualBatchExportService = manualBatchExportService;
    }

    /**
     * 获取导出结果。
     *
     * @param userIds 用户 ID 列表
     * @return 导出数据
     */
    @GetMapping("/rows")
    public Map<String, Object> rows(@RequestParam(value = "userIds", required = false) List<Long> userIds) {
        List<Long> effectiveUserIds = userIds == null || userIds.isEmpty()
                ? Arrays.asList(3001L, 3002L, 3003L, 3004L, 3005L)
                : userIds;
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userIds", effectiveUserIds);
        response.put("rows", manualBatchExportService.exportRows(effectiveUserIds));
        return response;
    }
}
