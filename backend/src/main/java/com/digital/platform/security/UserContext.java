package com.digital.platform.security;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;

/**
 * 当前登录用户的 ThreadLocal 持有者。
 * <p>
 * 请求进来时拦截器 set，请求结束必须 remove，否则线程池复用会造成
 * 用户信息串号（这是新手非常容易踩的坑）。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前登录用户，未登录返回 null。用于"登录可选"的接口。
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户，未登录直接抛 401。用于"必须登录"的接口。
     */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 获取当前登录用户 ID。业务层最常用。
     */
    public static Long userId() {
        return require().getUserId();
    }

    public static String username() {
        return require().getUsername();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && RoleType.isAdmin(user.getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
