package com.cmb.codeperf.demo.app.agentverify;

import com.cmb.codeperf.demo.common.domain.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Agent 验证 Mapper。
 * 使用真实 MyBatis Mapper 执行 SQL，确保 agent 能命中 Executor#query 并拿到 MappedStatement.id。
 */
@Mapper
public interface AgentOrderMapper {

    /**
     * 按用户 ID 查询订单。
     *
     * @param userId 用户 ID
     * @return 订单列表
     */
    @Select("select order_id, user_id, delivery_no, amount from agent_order where user_id = #{userId}")
    List<OrderDetail> selectByUserId(Long userId);
}
