package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.domain.User;
import com.codeperf.demo.integration.PromotionCache;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SafePrefetchDashboardService {

    private final UserRepository userRepository;
    private final PromotionCache promotionCache;

    public SafePrefetchDashboardService(UserRepository userRepository, PromotionCache promotionCache) {
        this.userRepository = userRepository;
        this.promotionCache = promotionCache;
    }

    public List<Map<String, Object>> buildDashboard(List<Long> userIds) {
        Map<Long, User> users = userRepository.findUsersByIds(userIds);
        List<Order> orders = userRepository.findOrdersByUserIds(userIds);
        Map<String, BigDecimal> discounts = promotionCache.getDiscounts(orders.stream()
                .map(Order::getItemName)
                .collect(Collectors.toSet()));

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Order order : orders) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", users.get(order.getUserId()));
            row.put("order", order);
            row.put("discount", discounts.get(order.getItemName()));
            rows.add(row);
        }
        return rows;
    }
}
