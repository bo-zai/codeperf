package com.codeperf.demo.app.controller;

import com.codeperf.demo.app.LocalDemoApplication;
import com.codeperf.demo.app.service.AppOrderPreviewService;
import com.codeperf.demo.app.service.DemoCheckoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * demo 订单接口。
 * 接口写法贴近真实项目：Controller 组装入参后调用多个业务服务完成页面聚合。
 */
@RestController
@RequestMapping("/demo/orders")
public class DemoOrderController {

    private final AppOrderPreviewService previewService;
    private final DemoCheckoutService checkoutService;

    public DemoOrderController(AppOrderPreviewService previewService, DemoCheckoutService checkoutService) {
        this.previewService = previewService;
        this.checkoutService = checkoutService;
    }

    /**
     * 查询订单预览数据。
     * 固定用户集合便于本地重复触发相同业务路径，保证演示数据稳定可复现。
     *
     * @return 订单预览、结算快照和本地场景摘要
     */
    @GetMapping("/preview")
    public Map<String, Object> preview() {
        List<Long> userIds = Arrays.asList(1001L, 1002L, 1003L);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("preview", previewService.preview(userIds));
        response.put("checkout", checkoutService.loadCheckoutSnapshot(userIds));
        response.put("local", LocalDemoApplication.runScenario());
        return response;
    }
}
