package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import com.codeperf.demo.integration.CustomerProfileClient;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderExportService {

    private final UserRepository userRepository;
    private final CustomerProfileClient customerProfileClient;

    public OrderExportService(UserRepository userRepository, CustomerProfileClient customerProfileClient) {
        this.userRepository = userRepository;
        this.customerProfileClient = customerProfileClient;
    }

    public List<Map<String, Object>> exportRows(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            User user = userRepository.findUserById(userId);
            List<Order> orders = userRepository.findOrdersByUserId(userId);
            Map<String, Object> profile = customerProfileClient.getProfile(userId);

            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("profile", profile);
            row.put("orderCount", orders.size());
            rows.add(row);
        }
        return rows;
    }
}
