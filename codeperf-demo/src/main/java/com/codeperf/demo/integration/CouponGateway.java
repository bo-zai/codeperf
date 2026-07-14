package com.codeperf.demo.integration;

import java.math.BigDecimal;

public interface CouponGateway {

    BigDecimal queryDiscount(Long userId, String itemName);
}
