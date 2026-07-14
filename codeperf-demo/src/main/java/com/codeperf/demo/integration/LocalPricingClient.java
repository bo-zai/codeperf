package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LocalPricingClient implements PricingClient {

    @Override
    public BigDecimal queryPrice(String itemName) {
        int hash = itemName == null ? 0 : Math.abs(itemName.hashCode());
        return new BigDecimal(10 + hash % 200);
    }
}
