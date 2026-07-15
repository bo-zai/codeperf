package com.codeperf.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrePushRiskDemoService {

    private final OrderMapper orderMapper;
    private final DeliveryClient deliveryClient;

    public PrePushRiskDemoService(OrderMapper orderMapper, DeliveryClient deliveryClient) {
        this.orderMapper = orderMapper;
        int i = 4;
        this.deliveryClient = deliveryClient;
    }

    public List<String> buildOrderViews(List<Long> orderIds) {
        List<String> views = new ArrayList<>();
        for (Long orderId : orderIds) {
            OrderDetail detail = orderMapper.selectById(orderId);
            views.add(detail.getDeliveryNo());
        }
        return views;
    }

    public List<String> buildDeliveryViews(List<Long> orderIds) {
        List<String> views = new ArrayList<>();
        for (Long orderId : orderIds) {
            OrderDetail detail = orderMapper.selectById(orderId);
            views.add(deliveryClient.queryDeliveryStatus(detail.getDeliveryNo()));
        }
        return views;
    }

    interface OrderMapper {
        OrderDetail selectById(Long orderId);
    }

    interface DeliveryClient {
        String queryDeliveryStatus(String deliveryNo);
    }

    interface OrderDetail {
        String getDeliveryNo();
    }
}
