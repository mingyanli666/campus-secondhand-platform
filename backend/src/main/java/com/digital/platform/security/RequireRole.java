package com.digital.platform.security;

import com.digital.platform.common.enums.RoleType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解。加在 Controller 类或方法上，拦截器会校验当前登录用户角色。
 * <p>
 * 例：{@code @RequireRole(RoleType.ADMIN)} 表示仅管理员可访问，
 * 普通用户即使带着合法 token 也会被拒绝（403），从根本上杜绝越权操作。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色，满足其一即可 */
    RoleType[] value();
}
