package com.codeperf.demo.app.infrastructure;

import com.codeperf.demo.common.domain.OrderDetail;
import com.codeperf.demo.common.repo.OrderMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * demo 订单 Mapper 实现。
 * 真实项目中这里通常由 MyBatis 代理生成，demo 中用内存数据模拟数据库访问。
 */
@Repository
public class DemoOrderMapper implements OrderMapper {

    /**
     * 按订单 ID 查询订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单详情
     */
    @Override
    public OrderDetail selectById(Long orderId) {
        return order(orderId, orderId - 10000L);
    }

    /**
     * 按用户 ID 查询订单列表。
     *
     * @param userId 用户 ID
     * @return 用户订单列表
     */
    @Override
    public List<OrderDetail> selectByUserId(Long userId) {
        List<OrderDetail> orders = new ArrayList<OrderDetail>();
        orders.add(order(userId + 10000L, userId));
        orders.add(order(userId + 20000L, userId));
        return orders;
    }

    private OrderDetail order(Long orderId, Long userId) {
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        detail.setUserId(userId);
        detail.setDeliveryNo("DLV-" + orderId);
        detail.setAmount(BigDecimal.valueOf(99.90D));
        return detail;
    }
}
