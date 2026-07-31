package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.app.repository.OrderQueryMapper;
import com.cmb.codeperf.demo.app.config.BusinessAudit;
import com.cmb.codeperf.demo.common.domain.OrderDetail;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单履约服务。
 * 该服务模拟履约页面按用户逐个补齐订单信息的真实写法，用于展示循环内数据库访问被数据量放大的风险。
 */
@Service
public class OrderFulfillmentService {

    private final OrderQueryMapper orderQueryMapper;

    public OrderFulfillmentService(OrderQueryMapper orderQueryMapper) {
        this.orderQueryMapper = orderQueryMapper;
    }

    /**
     * 构建履约预览行。
     *
     * @param userIds 用户 ID 列表
     * @return 履约预览行
     */
    @BusinessAudit
    public List<Map<String, Object>> buildFulfillmentRows(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            List<OrderDetail> orders = orderQueryMapper.selectByUserId(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("userId", userId);
            row.put("orderCount", orders.size());
            row.put("firstDeliveryNo", orders.isEmpty() ? "" : orders.get(0).getDeliveryNo());
            rows.add(row);
        }
        return rows;
    }
}
