package com.codeperf.demo.integration;

import java.math.BigDecimal;

public interface PaymentLedgerClient {

    BigDecimal queryPaidAmount(Long orderId);
}
