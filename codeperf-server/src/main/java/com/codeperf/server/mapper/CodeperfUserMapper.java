package com.codeperf.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeperf.server.model.entity.CodeperfUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CodeperfUserMapper extends BaseMapper<CodeperfUser> {
}
