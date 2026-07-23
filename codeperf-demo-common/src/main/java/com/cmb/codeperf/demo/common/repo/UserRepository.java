package com.cmb.codeperf.demo.common.repo;

import java.util.Map;

/**
 * 用户数据访问接口。
 */
public interface UserRepository {

    Map<String, Object> findUserById(Long userId);
}

