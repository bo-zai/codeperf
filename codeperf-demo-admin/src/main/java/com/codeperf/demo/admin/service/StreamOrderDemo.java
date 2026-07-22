package com.codeperf.demo.admin.service;

import java.util.Arrays;

public class StreamOrderDemo {

	public static void main(String[] args) {
		// 原始数组（注意这里有重复的 3 和 5）
		int[] numbers = {1, 2, 3, 5, 9, 7, 4, 3, 8, 6, 2, 5};

		// ========== 顺序流 ==========
		System.out.println("=== 顺序流 (Sequential) ===");
		Arrays.stream(numbers)                  // 创建顺序流
				.filter(n -> n % 2 != 0)          // 保留奇数
				.forEach(n -> System.out.print(n + " "));
		System.out.println("\n特点：严格按数组原有顺序输出奇数。");

		// ========== 并行流 ==========
		System.out.println("\n=== 并行流 (Parallel) ===");
		Arrays.stream(numbers)
				.parallel()                       // 转为并行流
				.filter(n -> n % 2 != 0)
				.forEach(n -> System.out.print(n + " "));
		System.out.println("\n特点：多线程并发处理，输出顺序通常乱序。");

		// ========== 展示并行流使用的线程 ==========
		System.out.println("\n=== 并行流线程信息 ===");
		Arrays.stream(numbers)
				.parallel()
				.filter(n -> n % 2 != 0)
				.forEach(n -> System.out.println(
						Thread.currentThread().getName() + " 处理: " + n
				));
	}
}