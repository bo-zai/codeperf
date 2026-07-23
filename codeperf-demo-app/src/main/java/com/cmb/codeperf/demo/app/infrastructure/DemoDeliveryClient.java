package com.cmb.codeperf.demo.app.infrastructure;

import com.cmb.codeperf.demo.common.client.DeliveryClient;
import org.springframework.stereotype.Component;

/**
 * demo 物流客户端。
 */
@Component
public class DemoDeliveryClient implements DeliveryClient {

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

