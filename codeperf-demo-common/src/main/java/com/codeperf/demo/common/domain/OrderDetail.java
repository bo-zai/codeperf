package com.codeperf.demo.common.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细。
 */
@Data
public class OrderDetail {

    /** 订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 物流单号 */
    private String deliveryNo;

    /** 订单金额 */
    private BigDecimal amount;
}
