package com.cmb.codeperf.demo.app;

import com.cmb.codeperf.demo.app.infrastructure.DemoCustomerProfileClient;
import com.cmb.codeperf.demo.app.infrastructure.DemoDeliveryClient;
import com.cmb.codeperf.demo.app.infrastructure.DemoOrderMapper;
import com.cmb.codeperf.demo.app.infrastructure.DemoUserRepository;
import com.cmb.codeperf.demo.app.service.AppOrderPreviewService;
import com.cmb.codeperf.demo.app.service.DemoCheckoutService;
import com.cmb.codeperf.demo.app.service.UserService;
import com.cmb.codeperf.demo.app.service.service1.BbkServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * demo-app 本地启动入口。
 * 该模块模拟用户端 Spring Boot 应用，提供订单预览与结算聚合的本地运行入口。
 */
@SpringBootApplication
public class LocalDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalDemoApplication.class, args);
    }

    /**
     * 运行一组本地业务场景。
     * 测试和手工验证共用该入口，避免同一业务流程在不同入口中重复实现。
     *
     * @return 场景执行摘要
     */
    public static ScenarioResult runScenario() {
        DemoOrderMapper orderMapper = new DemoOrderMapper();
        DemoCustomerProfileClient customerProfileClient = new DemoCustomerProfileClient();
        DemoDeliveryClient deliveryClient = new DemoDeliveryClient();
        DemoUserRepository userRepository = new DemoUserRepository();
        AppOrderPreviewService previewService = new AppOrderPreviewService(orderMapper, customerProfileClient);
        DemoCheckoutService checkoutService = new DemoCheckoutService(orderMapper, deliveryClient, userRepository);
        UserService userService = new UserService(new BbkServiceImpl());

        List<Long> userIds = Arrays.asList(1001L, 1002L, 1003L);
        List<Map<String, Object>> previewRows = previewService.preview(userIds);
        checkoutService.loadCheckoutSnapshot(userIds);
        int lookupCount = userService.getBbkIds(Arrays.asList("B001", "B002", "B003", "B004")).size();
        return new ScenarioResult(previewRows.size(), lookupCount);
    }

    /**
     * 本地场景执行结果。
     */
    @Getter
    @AllArgsConstructor
    public static class ScenarioResult {

        /** 订单预览行数 */
        private final int previewCount;

        /** 结算页补全信息数量 */
        private final int lookupCount;
    }
}

