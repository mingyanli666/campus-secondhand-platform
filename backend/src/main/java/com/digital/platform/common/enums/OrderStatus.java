package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 订单状态机。
 * <pre>
 *   待确认(0) --卖家确认--> 已确认(1) --双方见面交易完成--> 已完成(2)
 *       |                     |
 *       +--取消--> 已取消(3) <--+
 *       +--卖家拒绝/商品下架--> 已关闭(4)
 * </pre>
 */
@Getter
public enum OrderStatus {

    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    FINISHED(2, "已完成"),
    CANCELED(3, "已取消"),
    CLOSED(4, "已关闭");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }

    /**
     * 「进行中」的状态。
     * <p>
     * 二手商品是"一件"东西，不能同时卖给两个人。所以只要某商品存在
     * 待确认或已确认的订单，就不允许再有人对它下单。
     * 已完成/已取消/已关闭都是终态，不再占用商品。
     */
    public static java.util.List<Integer> activeCodes() {
        return java.util.List.of(PENDING.getCode(), CONFIRMED.getCode());
    }
}
