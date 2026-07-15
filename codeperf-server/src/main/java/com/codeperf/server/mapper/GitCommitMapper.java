package com.codeperf.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeperf.server.model.entity.GitCommit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GitCommitMapper extends BaseMapper<GitCommit> {
}
