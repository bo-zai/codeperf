package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LocalPaymentLedgerClient implements PaymentLedgerClient {

    @Override
    public BigDecimal queryPaidAmount(Long orderId) {
        if (orderId == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(20 + orderId % 300).setScale(2);
    }
}
