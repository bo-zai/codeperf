package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LocalPromotionCache implements PromotionCache {

    @Override
    public BigDecimal getDiscount(String itemName) {
        if (itemName == null || itemName.length() < 5) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("1.50");
    }

    @Override
    public Map<String, BigDecimal> getDiscounts(Iterable<String> itemNames) {
        Map<String, BigDecimal> discounts = new LinkedHashMap<String, BigDecimal>();
        for (String itemName : itemNames) {
            discounts.put(itemName, getDiscount(itemName));
        }
        return discounts;
    }
}
