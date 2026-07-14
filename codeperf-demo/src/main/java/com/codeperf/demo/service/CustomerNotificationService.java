package com.codeperf.demo.service;

import com.codeperf.demo.integration.CustomerProfileClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerNotificationService {

    private final CustomerProfileClient customerProfileClient;

    public CustomerNotificationService(CustomerProfileClient customerProfileClient) {
        this.customerProfileClient = customerProfileClient;
    }

    public List<Map<String, Object>> previewMessages(List<Long> userIds) {
        List<Map<String, Object>> previews = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            previews.add(buildPreview(userId));
        }
        return previews;
    }

    private Map<String, Object> buildPreview(Long userId) {
        Map<String, Object> profile = customerProfileClient.getProfile(userId);
        Map<String, Object> preview = new LinkedHashMap<String, Object>();
        preview.put("userId", userId);
        preview.put("profile", profile);
        preview.put("channel", userId != null && userId % 2 == 0 ? "SMS" : "EMAIL");
        return preview;
    }
}
