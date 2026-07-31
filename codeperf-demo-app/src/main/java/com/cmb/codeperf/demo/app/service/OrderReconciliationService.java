package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.common.client.CustomerProfileClient;
import com.cmb.codeperf.demo.common.domain.OrderDetail;
import com.cmb.codeperf.demo.common.repo.OrderMapper;
import com.cmb.codeperf.demo.common.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单对账服务。
 * 该服务保留真实项目中常见的批量聚合写法，用于展示循环内 I/O 被数据规模放大的业务风险。
 */
@Service
public class OrderReconciliationService {

    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final CustomerProfileClient customerProfileClient;

    public OrderReconciliationService(UserRepository userRepository,
                                      OrderMapper orderMapper,
                                      CustomerProfileClient customerProfileClient) {
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.customerProfileClient = customerProfileClient;
    }

    /**
     * 构建对账明细。
     *
     * @param userIds 用户 ID 列表
     * @return 对账明细列表
     */
    public List<Map<String, Object>> buildReconciliationRows(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            OrderDetail order = orderMapper.selectById(userId + 10000L);
            Map<String, Object> profile = customerProfileClient.getProfile(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("order", order);
            row.put("profile", profile);
            row.put("reconciliationStatus", "WAIT_CONFIRM");
            rows.add(row);
        }
        return rows;
    }
}
