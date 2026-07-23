package com.cmb.codeperf.demo.common.service;

import com.cmb.codeperf.demo.common.repo.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公共批量查询服务，模拟公共模块中被多个业务模块复用的代码。
 */
public class CommonBatchLookupService {

    private UserRepository userRepository;

    public List<Map<String, Object>> loadUsers(List<Long> userIds) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            users.add(userRepository.findUserById(userId));
        }
        return users;
    }
}

