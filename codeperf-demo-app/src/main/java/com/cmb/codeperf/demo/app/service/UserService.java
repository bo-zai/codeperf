package com.cmb.codeperf.demo.app.service;

import com.cmb.codeperf.demo.app.service.service1.BbkService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户业务服务。
 */
@Service
public class UserService {

    private final BbkService bbkService;

    public UserService(BbkService bbkService) {
        this.bbkService = bbkService;
    }

    /**
     * 根据班本课 ID 查询业务侧标识。
     *
     * @param bbkIds 班本课 ID 列表
     * @return 业务侧补全后的班本课标识
     */
    public List<String> getBbkIds(List<String> bbkIds) {
        List<String> values = new ArrayList<String>();
        for (String bbkId : bbkIds) {
            values.add(bbkService.getBbkId(bbkId));
        }
        return values;
    }
}

