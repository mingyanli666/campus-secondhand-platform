package com.digital.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义配置项映射，对应 application.yml 中的 platform.* 节点。
 * 用配置类而不是 @Value 逐个注入，好处是集中、可校验、IDE 有提示。
 */
@Data
@Component
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {

    private Jwt jwt = new Jwt();
    private File file = new File();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        /** 签名密钥，HS256 要求至少 32 字节 */
        private String secret;
        /** 有效期（小时） */
        private Integer expireHours = 72;
        /** 请求头名称 */
        private String header = "Authorization";
    }

    @Data
    public static class File {
        /** 上传根目录 */
        private String uploadPath = "./uploads/";
        /** 允许的图片后缀 */
        private String allowExt = "jpg,jpeg,png,gif,webp,bmp";
        /** 单文件最大字节数 */
        private Long maxSize = 10 * 1024 * 1024L;
    }

    @Data
    public static class Cors {
        /** 允许跨域的来源，逗号分隔 */
        private String allowedOrigins = "http://localhost:5173";
    }
}
