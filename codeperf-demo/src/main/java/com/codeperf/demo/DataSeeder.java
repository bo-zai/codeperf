package com.codeperf.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

/**
 * 建库后写入种子数据。规模常量集中在此，便于调整坑的可见度；
 * 改动后须同步更新 docs/01-demo-app.md 第 2 节的数值。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final int USER_COUNT = 50;
    private static final int MIN_ORDERS_PER_USER = 3;
    private static final int MAX_ORDERS_PER_USER = 8;
    private static final String[] ITEM_CANDIDATES = {
            "apple", "banana", "cherry", "date", "egg",
            "fish", "grape", "honey", "ice", "juice"
    };

    private final JdbcTemplate jdbc;

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Random rnd = new Random(20260623L); // 固定种子，数据可复现
        long orderId = 1;
        for (long uid = 1; uid <= USER_COUNT; uid++) {
            jdbc.update("INSERT INTO users(id, name) VALUES(?, ?)", uid, "user-" + uid);
            int orders = MIN_ORDERS_PER_USER
                    + rnd.nextInt(MAX_ORDERS_PER_USER - MIN_ORDERS_PER_USER + 1);
            for (int j = 0; j < orders; j++) {
                String item = ITEM_CANDIDATES[rnd.nextInt(ITEM_CANDIDATES.length)];
                BigDecimal amount = new BigDecimal(10 + rnd.nextInt(990)).setScale(2);
                jdbc.update(
                        "INSERT INTO orders(id, user_id, item_name, amount) VALUES(?, ?, ?, ?)",
                        orderId++, uid, item, amount);
            }
        }
    }
}
