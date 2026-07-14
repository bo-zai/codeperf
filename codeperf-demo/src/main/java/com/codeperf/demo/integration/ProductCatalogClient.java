package com.codeperf.demo.integration;

import java.util.Map;

public interface ProductCatalogClient {

    Map<String, Object> getProduct(String itemName);
}
