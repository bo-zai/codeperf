package com.codeperf.demo.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CustomerProfileClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CustomerProfileClient(RestTemplate restTemplate,
                                 @Value("${demo.customer-profile.base-url:http://localhost:8080}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> getProfile(Long userId) {
        return restTemplate.exchange(
                baseUrl + "/api/users/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();
    }
}
