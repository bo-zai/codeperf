package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LocalAuditDocumentStore implements AuditDocumentStore {

    @Override
    public Map<String, Object> findLatestAudit(Long orderId) {
        Map<String, Object> audit = new LinkedHashMap<String, Object>();
        audit.put("orderId", orderId);
        audit.put("operator", "system");
        audit.put("status", orderId != null && orderId % 3 == 0 ? "REVIEW" : "PASS");
        return audit;
    }
}
