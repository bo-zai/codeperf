package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.integration.AuditDocumentStore;
import com.codeperf.demo.integration.CustomerRiskClient;
import com.codeperf.demo.integration.PromotionCache;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BatchOrderAuditService {

    private final UserRepository userRepository;
    private final CustomerRiskClient customerRiskClient;
    private final PromotionCache promotionCache;
    private final AuditDocumentStore auditDocumentStore;

    public BatchOrderAuditService(UserRepository userRepository,
                                  CustomerRiskClient customerRiskClient,
                                  PromotionCache promotionCache,
                                  AuditDocumentStore auditDocumentStore) {
        this.userRepository = userRepository;
        this.customerRiskClient = customerRiskClient;
        this.promotionCache = promotionCache;
        this.auditDocumentStore = auditDocumentStore;
    }

    public List<Map<String, Object>> audit(List<Long> userIds) {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            List<Order> orders = userRepository.findOrdersByUserId(userId);
            int riskScore = customerRiskClient.queryRiskScore(userId);
            for (Order order : orders) {
                BigDecimal discount = promotionCache.getDiscount(order.getItemName());
                Map<String, Object> latestAudit = auditDocumentStore.findLatestAudit(order.getId());

                Map<String, Object> record = new LinkedHashMap<String, Object>();
                record.put("userId", userId);
                record.put("orderId", order.getId());
                record.put("riskScore", riskScore);
                record.put("discount", discount);
                record.put("latestAudit", latestAudit);
                records.add(record);
            }
        }
        return records;
    }
}
