package com.codeperf.demo.web;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserIdRequestParser {

    public List<Long> parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Arrays.asList(1L, 2L, 3L, 4L, 5L);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
