package com.cmb.codeperf.demo.admin.service;

public class NotifyReadCorrect {
    private static long count = 0;
    private static final Object LOCK = new Object();
    private static volatile boolean running = true;
    private static int readCounter = 0;

    public static void main(String[] args) throws Exception {
        // 读线程：等待通知，收到通知后立即读取
        Thread reader = new Thread(() -> {
            while (running) {
                synchronized (LOCK) {
                    try {
                        LOCK.wait(); // 阻塞等待写线程的通知
                        long val = count;
                        readCounter++;
                        // 每读取 1000 次打印一次，避免刷屏
                        if (readCounter % 1000 == 0) {
                            System.out.println("[读] 第 " + readCounter + " 次读取: count = " + val);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        // 写线程1：累加 100000 次，每次加完都通知
        // Thread w1 = new Thread(() -> {
        //     for (int i = 0; i < 100_000; i++) {
        //         synchronized (LOCK) {
        //             count++;
        //             LOCK.notify(); // 通知读线程
        //         }
        //         // 让出 CPU，给读线程机会去抢锁
        //         Thread.yield();
        //     }
        // });

        // 写线程2：累加 100000 次，每次加完都通知
        Thread w2 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                synchronized (LOCK) {
                    count++;
                    LOCK.notify();
                }
                Thread.yield();
            }
            // 写完了，通知读线程结束
            running = false;
            synchronized (LOCK) {
                LOCK.notify(); // 最后唤醒一次，让读线程退出循环
            }
        });

        reader.start();
        Thread.sleep(100); // 让读线程先进入 wait 状态

        long start = System.currentTimeMillis();
        // w1.start();
        w2.start();

        // w1.join();
        w2.join();
        long end = System.currentTimeMillis();

        // 等待读线程结束
        reader.join();

        System.out.println("===== 写线程耗时: " + (end - start) + " ms =====");
        System.out.println("【最终结果】count = " + count + "，读线程总共读取了 " + readCounter + " 次");
    }
}