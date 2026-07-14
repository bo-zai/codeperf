package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.integration.CouponGateway;
import com.codeperf.demo.integration.InventoryClient;
import com.codeperf.demo.integration.PricingClient;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderSettlementService {

    private final UserRepository userRepository;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final CouponGateway couponGateway;

    public OrderSettlementService(UserRepository userRepository,
                                  InventoryClient inventoryClient,
                                  PricingClient pricingClient,
                                  CouponGateway couponGateway) {
        this.userRepository = userRepository;
        this.inventoryClient = inventoryClient;
        this.pricingClient = pricingClient;
        this.couponGateway = couponGateway;
    }

    public Map<String, Object> settle(List<Long> userIds) {
        List<Order> orders = userRepository.findOrdersByUserIds(userIds);
        BigDecimal total = BigDecimal.ZERO;
        int reserved = 0;
        for (Order order : orders) {
            BigDecimal price = pricingClient.queryPrice(order.getItemName());
            BigDecimal discount = couponGateway.queryDiscount(order.getUserId(), order.getItemName());
            if (inventoryClient.reserve(order.getItemName(), 1)) {
                reserved++;
            }
            total = total.add(price).subtract(discount);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("orderCount", orders.size());
        result.put("reserved", reserved);
        result.put("total", total);
        return result;
    }
}
