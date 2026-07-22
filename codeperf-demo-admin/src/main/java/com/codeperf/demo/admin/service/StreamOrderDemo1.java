package com.codeperf.demo.admin.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamOrderDemo1 {

	public static void main(String[] args) {
		int[] nums = {1, 2, 3, 5, 9, 7, 4, 3, 8, 6, 2, 5};
		for (int i = 0; i < 100; i++) {
			List<Integer> result = Arrays.stream(nums).parallel()
					.filter(n -> n % 2 != 0)
					.mapToObj(n -> {
						try {
							// 故意让不同元素睡不同时间，打乱线程完成顺序
							Thread.sleep((long) (Math.random() * 10));
						} catch (InterruptedException e) {
						}
						return n;
					})
					.collect(Collectors.toList());
			System.out.println(result);  // 输出永远都是 [1, 3, 5, 9, 7, 3, 5]
		}
	}

}
