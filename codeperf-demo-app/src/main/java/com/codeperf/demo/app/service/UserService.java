package com.codeperf.demo.app.service;

import com.codeperf.demo.app.service.service1.BbkService;
import java.util.List;

public class UserService {
	BbkService bbkService;

	public void getBbkIds(List<String> bbkIds){
		for (String bbkId : bbkIds) {
			String bbkId1 = bbkService.getBbkId(bbkId);
		}
	}
}
