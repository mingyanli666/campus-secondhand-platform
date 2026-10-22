package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 用户角色。用枚举 + 常量代替散落的字符串字面量，改起来只改一处。
 */
@Getter
public enum RoleType {

    /** 普通用户：发布、浏览、咨询、收藏、下单 */
    USER("USER", "普通用户"),

    /** 平台管理员：审核、用户管理、分类维护、违规处理 */
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;

    RoleType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static boolean isAdmin(String role) {
        return ADMIN.code.equals(role);
    }
}
