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
 * 未提交购物车风险服务。
 * <p>
 * 用于验证开发者只改工作区文件、尚未 commit 时，手动执行 CodePerf scan 的识别效果。
 */
@Service
public class UncommittedCartRiskService {

    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public UncommittedCartRiskService(UserRepository userRepository, OrderMapper orderMapper) {
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * 批量构建购物车风险快照。
     *
     * @param userIds 用户 ID 列表
     * @return 购物车风险快照
     */
    public List<Map<String, Object>> buildCartSnapshots(List<Long> userIds) {
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            List<OrderDetail> orders = orderMapper.selectByUserId(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("orders", orders);
            row.put("source", "uncommitted-worktree");
            snapshots.add(row);
        }
        return snapshots;
    }
}
