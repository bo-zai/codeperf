package com.codeperf.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "codeperf.storage.mode=memory",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
public class CodePerfServerApplicationTest {

    @Test
    public void should_LoadApplicationContext_When_ServerStarts() {
    }
}
