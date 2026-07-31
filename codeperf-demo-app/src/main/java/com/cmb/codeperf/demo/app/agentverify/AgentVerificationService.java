package com.cmb.codeperf.demo.app.agentverify;

import com.cmb.codeperf.demo.common.domain.OrderDetail;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 验证业务服务。
 * 方法外层由注解式 Spring AOP 包裹，方法内部保留循环内 MyBatis 查询，用于手工验证动态 I/O 佐证能力。
 */
@Service
public class AgentVerificationService {

    private final AgentOrderMapper agentOrderMapper;

    public AgentVerificationService(AgentOrderMapper agentOrderMapper) {
        this.agentOrderMapper = agentOrderMapper;
    }

    /**
     * 构建订单行数据。
     *
     * @param userIds 用户 ID 列表
     * @return 订单行
     */
    @AgentAopTrace
    public List<Map<String, Object>> buildAopMybatisRows(List<Long> userIds) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long userId : userIds) {
            List<OrderDetail> orders = agentOrderMapper.selectByUserId(userId);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("userId", userId);
            row.put("orderCount", orders.size());
            row.put("firstDeliveryNo", orders.isEmpty() ? "" : orders.get(0).getDeliveryNo());
            rows.add(row);
        }
        return rows;
    }
}
