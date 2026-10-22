package com.digital.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.digital.platform.mapper")
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
