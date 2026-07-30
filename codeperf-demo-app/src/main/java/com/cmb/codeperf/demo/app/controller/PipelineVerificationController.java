package com.cmb.codeperf.demo.app.controller;

import com.cmb.codeperf.demo.app.service.PipelineVerificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线联调验证接口。
 * 用于验证“本地 push 静态扫描任务”和“应用运行期 agent 动态证据”能否按同一 Git 身份关联。
 */
@RestController
@RequestMapping("/demo/pipeline")
public class PipelineVerificationController {

    private final PipelineVerificationService pipelineVerificationService;

    public PipelineVerificationController(PipelineVerificationService pipelineVerificationService) {
        this.pipelineVerificationService = pipelineVerificationService;
    }

    /**
     * 生成订单对账数据。
     * 该接口模拟真实业务中的批量对账页面：前端传入一批用户，后端逐个补齐订单与画像信息。
     *
     * @param userIds 用户 ID 列表，未传时使用固定演示数据
     * @return 对账明细
     */
    @GetMapping("/orders/reconciliation")
    public Map<String, Object> reconciliation(@RequestParam(value = "userIds", required = false) List<Long> userIds) {
        List<Long> effectiveUserIds = userIds == null || userIds.isEmpty()
                ? Arrays.asList(3101L, 3102L, 3103L, 3104L, 3105L)
                : userIds;
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("userIds", effectiveUserIds);
        response.put("rows", pipelineVerificationService.buildReconciliationRows(effectiveUserIds));
        return response;
    }
}
