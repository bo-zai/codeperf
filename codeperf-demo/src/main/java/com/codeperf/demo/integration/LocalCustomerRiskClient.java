package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

@Component
public class LocalCustomerRiskClient implements CustomerRiskClient {

    @Override
    public int queryRiskScore(Long userId) {
        if (userId == null) {
            return 0;
        }
        return (int) ((userId * 17) % 100);
    }
}
