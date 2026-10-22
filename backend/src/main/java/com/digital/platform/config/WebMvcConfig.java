package com.digital.platform.config;

import com.digital.platform.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web 层通用配置：拦截器注册、跨域、静态资源映射。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final PlatformProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 只拦截业务接口，静态资源、接口文档不受影响
                .addPathPatterns("/api/**");
    }

    /**
     * 把本地上传目录暴露成静态资源，前端通过 /uploads/xxx.jpg 直接访问图片。
     * 图片存本地最快、最省钱，也避免依赖第三方服务。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = properties.getFile().getUploadPath();
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            // 目录创建失败不阻断启动，上传时会再次报错提示
            System.err.println("上传目录创建失败：" + dir.getAbsolutePath());
        }
        String location = "file:" + dir.getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    /**
     * 跨域配置。前后端分离开发时前端跑在 5173，后端 8080，必须显式放行。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(properties.getCors().getAllowedOrigins().split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
