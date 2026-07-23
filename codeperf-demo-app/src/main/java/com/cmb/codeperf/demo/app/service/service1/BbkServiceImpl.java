package com.cmb.codeperf.demo.app.service.service1;

import org.springframework.stereotype.Service;

/**
 * 班本课服务实现，模拟真实项目中 Service 类型的跨层查询。
 */
@Service
public class BbkServiceImpl implements BbkService {

    /**
     * 查询班本课业务标识。
     *
     * @param bbkId 班本课 ID
     * @return 业务侧班本课标识
     */
    @Override
    public String getBbkId(String bbkId) {
        return "bbk-" + bbkId;
    }
}

