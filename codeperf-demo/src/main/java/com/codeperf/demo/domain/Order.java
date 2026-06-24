package com.codeperf.demo.domain;

import java.math.BigDecimal;

/** 订单 POJO。 */
public class Order {
    private Long id;
    private Long userId;
    private String itemName;
    private BigDecimal amount;

    public Order() {
    }

    public Order(Long id, Long userId, String itemName, BigDecimal amount) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
