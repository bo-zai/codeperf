package com.cmb.codeperf.demo.app.repository;

import com.cmb.codeperf.demo.common.domain.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单查询 Mapper。
 * 使用真实 MyBatis Mapper 执行 SQL，便于本地应用完整覆盖 Controller、AOP、Service、Mapper、SQL 链路。
 */
@Mapper
public interface OrderQueryMapper {

    /**
     * 按用户 ID 查询订单。
     *
     * @param userId 用户 ID
     * @return 订单列表
     */
    @Select("select order_id, user_id, delivery_no, amount from app_order where user_id = #{userId}")
    List<OrderDetail> selectByUserId(Long userId);
}
