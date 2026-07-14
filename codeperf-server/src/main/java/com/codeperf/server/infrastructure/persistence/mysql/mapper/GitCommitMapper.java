package com.codeperf.server.infrastructure.persistence.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeperf.server.infrastructure.persistence.mysql.entity.GitCommitEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GitCommitMapper extends BaseMapper<GitCommitEntity> {
}
