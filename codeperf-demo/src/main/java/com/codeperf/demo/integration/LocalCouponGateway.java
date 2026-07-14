package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LocalCouponGateway implements CouponGateway {

    @Override
    public BigDecimal queryDiscount(Long userId, String itemName) {
        if (userId == null || userId % 5 != 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("3.00");
    }
}
