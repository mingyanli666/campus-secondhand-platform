package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 审核动作。审核记录表用它区分"管理员做了什么"，
 * 强制下架也算一种审核动作，这样商品的每一次状态变更都能在记录里查到。
 */
@Getter
public enum AuditAction {

    /** 审核通过，商品上架 */
    PASS(1, "审核通过"),

    /** 审核驳回，商品退回卖家修改 */
    REJECT(2, "审核驳回"),

    /** 管理员强制下架（违规处理） */
    FORCE_OFF(3, "强制下架");

    private final Integer code;
    private final String desc;

    AuditAction(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        for (AuditAction action : values()) {
            if (action.code.equals(code)) {
                return action.desc;
            }
        }
        return "未知操作";
    }
}
