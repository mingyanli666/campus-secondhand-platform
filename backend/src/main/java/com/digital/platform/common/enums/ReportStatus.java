package com.digital.platform.common.enums;

import lombok.Getter;

/**
 * 举报处理状态。
 * <p>
 * 「已处理」和「已驳回」要分开：它们对举报人的含义完全不同 ——
 * 前者是"你说得对，我们已经处置了"，后者是"我们核实过，这个商品没问题"。
 * 合并成一个"已结束"会让举报人觉得自己的反馈被无视了。
 */
@Getter
public enum ReportStatus {

    PENDING(0, "待处理"),
    HANDLED(1, "已处理"),
    REJECTED(2, "已驳回");

    private final Integer code;
    private final String desc;

    ReportStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        for (ReportStatus status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }
}
