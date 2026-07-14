package com.codeperf.demo.integration;

import java.math.BigDecimal;

public interface PricingClient {

    BigDecimal queryPrice(String itemName);
}
