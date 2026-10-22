package com.digital.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。
 * <p>
 * {@code @EnableScheduling} 用于开启定时任务 —— 浏览量缓存在 Redis 里累加，
 * 由 {@code ViewCountFlushTask} 定期批量回写到数据库。
 */
@SpringBootApplication
@MapperScan("com.digital.platform.mapper")
@EnableScheduling
public class DigitalPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalPlatformApplication.class, args);
        System.out.println("""

                ==========================================================
                  二手数码产品信息发布与交流平台 后端服务启动成功
                  接口文档: http://localhost:8080/doc.html
                ==========================================================
                """);
    }
}
