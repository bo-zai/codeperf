package com.codeperf.demo.app.service;

import com.codeperf.demo.common.client.CustomerProfileClient;
import com.codeperf.demo.common.domain.OrderDetail;
import com.codeperf.demo.common.repo.OrderMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端订单预览服务。
 */
public class AppOrderPreviewService {

    private OrderMapper orderMapper;
    private CustomerProfileClient customerProfileClient;

    public List<Map<String, Object>> preview(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            List<OrderDetail> orders = orderMapper.selectByUserId(userId);
            Map<String, Object> profile = customerProfileClient.getProfile(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("orders", orders);
            row.put("profile", profile);
            rows.add(row);
        }
        return rows;
    }
}
