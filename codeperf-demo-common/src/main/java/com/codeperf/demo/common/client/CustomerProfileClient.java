package com.codeperf.demo.common.client;

import java.util.Map;

/**
 * 用户画像外部服务客户端。
 */
public interface CustomerProfileClient {

    Map<String, Object> getProfile(Long userId);
}
