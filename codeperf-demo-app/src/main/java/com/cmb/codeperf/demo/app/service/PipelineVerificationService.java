package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.common.client.CustomerProfileClient;
import com.cmb.codeperf.demo.common.domain.OrderDetail;
import com.cmb.codeperf.demo.common.repo.OrderMapper;
import com.cmb.codeperf.demo.common.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线联调验证服务。
 * 该服务保留真实项目中常见的批量聚合写法，便于验证 CodePerf 对循环内 I/O 放大的识别和动态佐证。
 */
@Service
public class PipelineVerificationService {

    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final CustomerProfileClient customerProfileClient;

    public PipelineVerificationService(UserRepository userRepository,
                                       OrderMapper orderMapper,
                                       CustomerProfileClient customerProfileClient) {
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.customerProfileClient = customerProfileClient;
    }

    /**
     * 构建对账明细。
     * 这里故意保留循环内访问 Mapper、Repository、外部 Client 的真实风险形态，用于完整联调静态和动态结果关联。
     *
     * @param userIds 用户 ID 列表
     * @return 对账明细列表
     */
    public List<Map<String, Object>> buildReconciliationRows(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            Map<String, Object> user = userRepository.findUserById(userId);
            OrderDetail order = orderMapper.selectById(userId + 10000L);
            Map<String, Object> profile = customerProfileClient.getProfile(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("user", user);
            row.put("order", order);
            row.put("profile", profile);
            row.put("reconciliationStatus", "WAIT_CONFIRM");
            row.put("reconciliationStatus1", "WAIT_CONFIRM");
            row.put("reconciliationStatus2", "WAIT_CONFIRM");
            rows.add(row);
        }
        return rows;
    }
}
