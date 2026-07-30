package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.common.repo.OrderMapper;
import com.cmb.codeperf.demo.common.repo.UserRepository;
import com.cmb.codeperf.demo.common.domain.OrderDetail;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量订单风险查询服务。
 * <p>
 * 这个服务模拟真实业务里的批量导出场景：先拿到一批用户，再逐个补齐用户和订单信息。
 * 业务写法本身是常见的，但当用户量变大时，循环内的多次数据库访问会被放大。
 */
@Service
public class BulkOrderRiskService {

    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public BulkOrderRiskService(UserRepository userRepository, OrderMapper orderMapper) {
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * 构建批量订单风险明细。
     *
     * @param userIds 用户 ID 列表
     * @return 每个用户对应的订单和用户信息
     */
    public List<Map<String, Object>> buildRiskOverview(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            OrderDetail order = orderMapper.selectById(userId + 10000L);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("order", order);
            row.put("riskHint", "循环内逐个补齐用户与订单信息");
            rows.add(row);
        }
        return rows;
    }
}
