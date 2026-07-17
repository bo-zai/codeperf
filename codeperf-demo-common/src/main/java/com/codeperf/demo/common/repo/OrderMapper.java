package com.codeperf.demo.common.repo;

import com.codeperf.demo.common.domain.OrderDetail;

import java.util.List;

/**
 * 订单数据访问接口。
 */
public interface OrderMapper {

    OrderDetail selectById(Long orderId);

    List<OrderDetail> selectByUserId(Long userId);
}
