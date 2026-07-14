package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.integration.PaymentLedgerClient;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {

    private final UserRepository userRepository;
    private final PaymentLedgerClient paymentLedgerClient;

    public ReconciliationService(UserRepository userRepository, PaymentLedgerClient paymentLedgerClient) {
        this.userRepository = userRepository;
        this.paymentLedgerClient = paymentLedgerClient;
    }

    public List<Map<String, Object>> reconcile(List<Long> userIds) {
        List<Map<String, Object>> differences = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            List<Order> orders = userRepository.findOrdersByUserId(userId);
            for (Order order : orders) {
                BigDecimal paidAmount = paymentLedgerClient.queryPaidAmount(order.getId());
                if (order.getAmount().compareTo(paidAmount) != 0) {
                    Map<String, Object> difference = new LinkedHashMap<String, Object>();
                    difference.put("userId", userId);
                    difference.put("orderId", order.getId());
                    difference.put("orderAmount", order.getAmount());
                    difference.put("paidAmount", paidAmount);
                    differences.add(difference);
                }
            }
        }
        return differences;
    }
}
