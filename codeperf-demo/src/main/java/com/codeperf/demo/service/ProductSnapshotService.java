package com.codeperf.demo.service;

import com.codeperf.demo.domain.Order;
import com.codeperf.demo.integration.ProductCatalogClient;
import com.codeperf.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductSnapshotService {

    private final UserRepository userRepository;
    private final ProductCatalogClient productCatalogClient;

    public ProductSnapshotService(UserRepository userRepository, ProductCatalogClient productCatalogClient) {
        this.userRepository = userRepository;
        this.productCatalogClient = productCatalogClient;
    }

    public List<Map<String, Object>> buildSnapshots(List<Long> userIds) {
        List<Order> orders = userRepository.findOrdersByUserIds(userIds);
        return orders.stream()
                .map(order -> productCatalogClient.getProduct(order.getItemName()))
                .collect(Collectors.toList());
    }
}
