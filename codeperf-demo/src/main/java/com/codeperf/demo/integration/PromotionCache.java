package com.codeperf.demo.integration;

import java.math.BigDecimal;
import java.util.Map;

public interface PromotionCache {

    BigDecimal getDiscount(String itemName);

    Map<String, BigDecimal> getDiscounts(Iterable<String> itemNames);
}
