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
 * 订单导出服务。
 * 这个写法贴近真实业务：先拿一批用户，再逐个查询订单和物流状态做导出。
 */
@Service
public class OrderExportService {

    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final DeliveryClient deliveryClient;

    public OrderExportService(UserRepository userRepository, OrderMapper orderMapper, DeliveryClient deliveryClient) {
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.deliveryClient = deliveryClient;
    }

    /**
     * 构造订单导出数据。
     *
     * @param userIds 用户 ID 列表
     * @return 订单导出行
     */
    public List<Map<String, Object>> exportRows(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            List<OrderDetail> orders = orderMapper.selectByUserId(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("orders", orders);
            row.put("deliveryState", resolveDeliveryState(orders));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 从订单列表里取一个物流单号，再访问物流接口补齐状态。
     * 这种把外部 I/O 放在辅助方法里的写法在真实项目中很常见。
     */
    private String resolveDeliveryState(List<OrderDetail> orders) {
        if (orders == null || orders.isEmpty()) {
            return "EMPTY";
        }
        OrderDetail first = orders.get(0);
        if (first == null || first.getDeliveryNo() == null) {
            return "UNKNOWN";
        }
        return deliveryClient.queryDeliveryStatus(first.getDeliveryNo());
    }
}
