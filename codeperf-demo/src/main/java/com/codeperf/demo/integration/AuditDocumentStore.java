package com.codeperf.demo.integration;

import java.util.Map;

public interface AuditDocumentStore {

    Map<String, Object> findLatestAudit(Long orderId);
}
