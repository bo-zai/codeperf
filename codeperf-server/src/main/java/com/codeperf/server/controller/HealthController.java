package com.codeperf.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务健康探针接口。
 * 保持无依赖、轻量响应，避免数据库或下游服务短暂波动影响容器存活探测。
 */
@RestController
public class HealthController {

    /**
     * 返回服务基础存活状态。
     *
     * @return 固定 OK，供负载均衡或容器平台做 HTTP 探活
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
