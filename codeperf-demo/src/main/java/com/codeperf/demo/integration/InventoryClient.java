package com.codeperf.demo.integration;

public interface InventoryClient {

    boolean reserve(String itemName, int quantity);
}
