package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

@Component
public class LocalInventoryClient implements InventoryClient {

    @Override
    public boolean reserve(String itemName, int quantity) {
        return itemName != null && quantity > 0;
    }
}
