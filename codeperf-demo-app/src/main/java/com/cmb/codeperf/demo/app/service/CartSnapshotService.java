package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.common.domain.OrderDetail;
import com.cmb.codeperf.demo.common.repo.OrderMapper;
import com.cmb.codeperf.demo.common.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车快照服务。
 * 该服务模拟购物车页面批量补齐用户和订单信息的业务流程。
 */
@Service
public class CartSnapshotService {

    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public CartSnapshotService(UserRepository userRepository, OrderMapper orderMapper) {
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * 批量构建购物车快照。
     *
     * @param userIds 用户 ID 列表
     * @return 购物车快照
     */
    public List<Map<String, Object>> buildCartSnapshots(List<Long> userIds) {
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            List<OrderDetail> orders = orderMapper.selectByUserId(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("orders", orders);
            row.put("source", "cart-page");
            snapshots.add(row);
        }
        return snapshots;
    }
}
