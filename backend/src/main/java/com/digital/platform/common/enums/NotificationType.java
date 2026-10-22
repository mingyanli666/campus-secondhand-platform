package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 站内通知类型。
 */
@Getter
public enum NotificationType {

    /** 商品审核结果（通过 / 驳回） */
    AUDIT(1, "审核结果"),

    /** 订单状态变化 */
    ORDER(2, "订单状态"),

    /** 有人回复了你的留言 */
    COMMENT(3, "留言回复"),

    /** 系统公告 / 违规处理 */
    SYSTEM(4, "系统通知");

    private final Integer code;
    private final String desc;

    NotificationType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
