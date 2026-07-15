package com.codeperf.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeperf.server.model.entity.RuleDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则定义数据访问接口。
 */
@Mapper
public interface RuleDefinitionMapper extends BaseMapper<RuleDefinition> {
}
