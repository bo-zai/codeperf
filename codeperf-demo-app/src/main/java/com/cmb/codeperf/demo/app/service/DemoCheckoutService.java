package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.common.client.DeliveryClient;
import com.cmb.codeperf.demo.common.domain.OrderDetail;
import com.cmb.codeperf.demo.common.repo.OrderMapper;
import com.cmb.codeperf.demo.common.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结算页聚合服务。
 * 结算页需要聚合用户、订单和物流信息，用于模拟真实业务中的多数据源补全流程。
 */
@Service
public class DemoCheckoutService {

    private final OrderMapper orderMapper;
    private final DeliveryClient deliveryClient;
    private final UserRepository userRepository;

    public DemoCheckoutService(OrderMapper orderMapper, DeliveryClient deliveryClient, UserRepository userRepository) {
        this.orderMapper = orderMapper;
        this.deliveryClient = deliveryClient;
        this.userRepository = userRepository;
    }

    /**
     * 加载结算页快照。
     *
     * @param userIds 用户 ID 列表
     * @return 结算页聚合后的用户、订单和物流信息
     */
    public List<Map<String, Object>> loadCheckoutSnapshot(List<Long> userIds) {
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            OrderDetail order = orderMapper.selectById(userId + 10000L);
            String deliveryStatus = deliveryClient.queryDeliveryStatus(order.getDeliveryNo());
            snapshots.add(toSnapshot(user, order, deliveryStatus));
        }
        return snapshots;
    }

    private Map<String, Object> toSnapshot(Map<String, Object> user, OrderDetail order, String deliveryStatus) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("user", user);
        row.put("order", order);
        row.put("deliveryStatus", deliveryStatus);
        return row;
    }
}

