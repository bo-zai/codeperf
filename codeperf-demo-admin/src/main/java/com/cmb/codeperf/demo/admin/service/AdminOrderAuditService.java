package com.cmb.codeperf.demo.admin.service;

import com.cmb.codeperf.demo.common.client.DeliveryClient;
import com.cmb.codeperf.demo.common.domain.OrderDetail;
import com.cmb.codeperf.demo.common.repo.OrderMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台订单审核服务。
 */
public class AdminOrderAuditService {

    private OrderMapper orderMapper;
    private DeliveryClient deliveryClient;

    public List<String> auditDeliveries(List<Long> orderIds) {
        List<String> statuses = new ArrayList<String>();
        for (Long orderId : orderIds) {
            OrderDetail detail = orderMapper.selectById(orderId);
            statuses.add(deliveryClient.queryDeliveryStatus(detail.getDeliveryNo()));
        }
        return statuses;
    }
}

