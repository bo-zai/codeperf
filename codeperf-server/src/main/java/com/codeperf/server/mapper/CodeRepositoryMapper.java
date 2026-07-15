package com.codeperf.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeperf.server.model.entity.CodeRepository;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CodeRepositoryMapper extends BaseMapper<CodeRepository> {
}
