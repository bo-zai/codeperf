package com.cmb.codeperf.demo.admin.service;

public class NoLockDemo {

	private static long count = 0;
	private static final Object LOCK = new Object();

	// 写操作：加锁保证累加准确
	public void addCount() {
		// 写多一点（200万次），让写线程运行时间超过1秒，给JIT留出编译读线程的时间
		for (int i = 0; i < 2_000_000; i++) {
			synchronized (LOCK) {
				count++;
			}
		}
	}

	public long readCount() {
		return count;
	}

	public static void main(String[] args) throws Exception {
		NoLockDemo demo = new NoLockDemo();

		// 1. 先启动读线程（不加锁，死循环检测）
		Thread reader = new Thread(() -> {
			System.out.println("[场景1] 读线程结束，count = " + demo.readCount());
		});
		reader.start();

		// 2. 主线程休眠1.5秒，让读线程的空循环跑足够多次，触发JIT即时编译
		Thread.sleep(1500);

		// 3. 启动两个写线程
		Thread a = new Thread(() -> demo.addCount());
		Thread b = new Thread(() -> demo.addCount());
		a.start();
		b.start();

		// 4. 等待写线程完成
		a.join();
		b.join();
		System.out.println("[主线程] 写线程执行完毕，count = " + count);

		// 5. 等待读线程结束 —— 如果被JIT优化卡死，程序将永远阻塞在这里，完美演示可见性失效
		reader.join();
	}
}