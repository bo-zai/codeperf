package com.cmb.codeperf.demo.admin.service;

public class CumulateCase {
    private static long count = 0;
    private static final Object WRITE_LOCK = new Object(); // 写锁

    // 写操作（加锁保证原子性）
    public void add100k() {
        int idx = 0;
        while (idx++ < 100000) {
            synchronized (WRITE_LOCK) {
                count++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        CumulateCase test = new CumulateCase();
        Thread a = new Thread(() -> test.add100k());
        Thread b = new Thread(() -> test.add100k());

        // ========== 场景1：线程3 读取时不加任何锁 ==========
        Thread reader1 = new Thread(() -> {
            System.out.println("【场景1】无锁读取线程启动");
            // 危险！没有同步，JIT可能将 count < 200000 外提为常量，导致死循环
            while (count < 200000) {
                // 空转，如果死循环，说明看不到写入的值
                Thread.yield();
            }
            System.out.println("【场景1】无锁读取线程结束，count = " + count);
        });

        // 启动三个线程
        reader1.start();  // 先启动读线程
        a.start();
        b.start();

        a.join();
        b.join();
        // 等待读线程结束（如果它读不到正确值，这里会一直阻塞，程序无法结束，直观展示错误）
        reader1.join(); 
        System.out.println("主线程结束，最终 count = " + count);
    }
}