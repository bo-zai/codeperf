package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SafeBatchOrderService {

    private final UserRepository userRepository;

    public SafeBatchOrderService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> buildRows(List<Long> userIds) {
        Map<Long, User> users = userRepository.findUsersByIds(userIds);
        Map<Long, List<Order>> ordersByUser = userRepository.findOrdersByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(Order::getUserId));

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", users.get(userId));
            row.put("orders", ordersByUser.getOrDefault(userId, new ArrayList<Order>()));
            rows.add(row);
        }
        return rows;
    }
}
