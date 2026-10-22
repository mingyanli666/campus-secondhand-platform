package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 举报类型。
 * <p>
 * 为什么要把举报分类，而不是只让用户写一段文字？
 * <p>
 * 因为**分类决定了处理的优先级和方式**：
 * 违禁物品要立刻下架，价格欺诈要先核对价格，虚假信息要对比描述和图片。
 * 一堆没有分类的文字描述，管理员只能一条条读；有了分类就能按类型批量判断。
 */
@Getter
public enum ReportType {

    FAKE(1, "虚假信息"),
    ILLEGAL(2, "违禁物品"),
    PRICE_FRAUD(3, "价格欺诈"),
    OTHER(4, "其他");

    private final Integer code;
    private final String desc;

    ReportType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return type.desc;
            }
        }
        return "其他";
    }
}
