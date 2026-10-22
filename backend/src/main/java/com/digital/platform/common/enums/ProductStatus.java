package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 商品状态机。
 * <pre>
 *   待审核(0) --审核通过--> 已上架(1) --卖家下架--> 已下架(2) --重新上架--> 待审核(0)
 *                            |  --买家下单成交--> 已售出(3)
 *   待审核(0) --审核驳回--> 审核驳回(4) --编辑后重新提交--> 待审核(0)
 * </pre>
 * 状态流转集中在 Service 层校验，禁止随意跨状态跳转。
 */
@Getter
public enum ProductStatus {

    PENDING(0, "待审核"),
    ON_SALE(1, "已上架"),
    OFF_SHELF(2, "已下架"),
    SOLD(3, "已售出"),
    REJECTED(4, "审核驳回");

    private final Integer code;
    private final String desc;

    ProductStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        for (ProductStatus status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }
}
