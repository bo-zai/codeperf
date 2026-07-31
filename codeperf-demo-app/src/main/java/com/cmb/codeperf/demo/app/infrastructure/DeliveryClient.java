package com.cmb.codeperf.demo.app.infrastructure;

import org.springframework.stereotype.Component;

/**
 * 本地物流客户端。
 * 真实项目通常会通过 HTTP 或 RPC 查询物流系统，demo 使用本地返回值保持场景可重复。
 */
@Component
public class DeliveryClient implements com.cmb.codeperf.demo.common.client.DeliveryClient {

    /**
     * 查询物流状态。
     *
     * @param deliveryNo 物流单号
     * @return 物流状态描述
     */
    @Override
    public String queryDeliveryStatus(String deliveryNo) {
        return "SIGNED:" + deliveryNo;
    }
}
