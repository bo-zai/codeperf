package com.cmb.codeperf.demo.app;

import com.cmb.codeperf.demo.app.course.LocalLearningCourseClient;
import com.cmb.codeperf.demo.app.infrastructure.CustomerProfileClient;
import com.cmb.codeperf.demo.app.infrastructure.DeliveryClient;
import com.cmb.codeperf.demo.app.infrastructure.OrderMapper;
import com.cmb.codeperf.demo.app.infrastructure.UserRepository;
import com.cmb.codeperf.demo.app.service.CheckoutSnapshotService;
import com.cmb.codeperf.demo.app.service.OrderPreviewService;
import com.cmb.codeperf.demo.app.service.UserService;
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
     * 测试和本地启动入口共用该流程，避免同一业务场景在不同入口中重复实现。
     *
     * @return 场景执行摘要
     */
    public static ScenarioResult runScenario() {
        OrderMapper orderMapper = new OrderMapper();
        CustomerProfileClient customerProfileClient = new CustomerProfileClient();
        DeliveryClient deliveryClient = new DeliveryClient();
        UserRepository userRepository = new UserRepository();
        OrderPreviewService previewService = new OrderPreviewService(orderMapper, customerProfileClient);
        CheckoutSnapshotService checkoutService = new CheckoutSnapshotService(orderMapper, deliveryClient, userRepository);
        UserService userService = new UserService(new LocalLearningCourseClient());

        List<Long> userIds = Arrays.asList(1001L, 1002L, 1003L);
        List<Map<String, Object>> previewRows = previewService.preview(userIds);
        checkoutService.loadCheckoutSnapshot(userIds);
        int lookupCount = userService.getCourseCodes(Arrays.asList("C001", "C002", "C003", "C004")).size();
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

        /** 课程补全信息数量 */
        private final int lookupCount;
    }
}

