package com.codeperf.demo.common.client;

/**
 * 物流外部服务客户端。
 */
public interface DeliveryClient {

    String queryDeliveryStatus(String deliveryNo);
}
