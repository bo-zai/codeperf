package com.cmb.codeperf.demo.app.infrastructure;

import com.cmb.codeperf.demo.common.client.CustomerProfileClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo 用户画像客户端。
 * 真实项目中这里通常是 HTTP/RPC/SDK 客户端，demo 用固定返回值模拟远程调用。
 */
@Component
public class DemoCustomerProfileClient implements CustomerProfileClient {

    /**
     * 获取用户画像。
     *
     * @param userId 用户 ID
     * @return 用户画像字段
     */
    @Override
    public Map<String, Object> getProfile(Long userId) {
        Map<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("userId", userId);
        profile.put("level", "VIP");
        return profile;
    }
}

