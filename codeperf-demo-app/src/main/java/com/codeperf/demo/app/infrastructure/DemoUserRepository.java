package com.codeperf.demo.app.infrastructure;

import com.codeperf.demo.common.repo.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo 用户仓储。
 * 真实项目中这里通常由 JPA/MyBatis 或内部 DAO 实现，demo 用内存数据模拟数据库访问。
 */
@Repository
public class DemoUserRepository implements UserRepository {

    /**
     * 按用户 ID 查询用户基础信息。
     *
     * @param userId 用户 ID
     * @return 用户基础信息
     */
    @Override
    public Map<String, Object> findUserById(Long userId) {
        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("userId", userId);
        user.put("name", "用户-" + userId);
        return user;
    }
}
