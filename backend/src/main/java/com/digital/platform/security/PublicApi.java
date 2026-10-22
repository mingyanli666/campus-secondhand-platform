package com.digital.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口为公开访问，不需要登录。
 * <p>
 * 相比在配置里维护一长串 excludePathPatterns，注解方式贴在方法上更直观，
 * 看接口就知道它要不要登录，也不怕新增接口时忘记加白名单。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicApi {
}
