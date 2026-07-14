package com.codeperf.demo.integration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LocalProductCatalogClient implements ProductCatalogClient {

    @Override
    public Map<String, Object> getProduct(String itemName) {
        Map<String, Object> product = new LinkedHashMap<String, Object>();
        product.put("name", itemName);
        product.put("category", itemName == null || itemName.length() % 2 == 0 ? "food" : "fresh");
        product.put("enabled", Boolean.TRUE);
        return product;
    }
}
