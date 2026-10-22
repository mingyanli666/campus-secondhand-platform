package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单信息")
public class OrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    private Long productId;

    @Schema(description = "商品标题（下单时快照）")
    private String productTitle;

    @Schema(description = "商品封面（下单时快照）")
    private String productCover;

    @Schema(description = "成交价（下单时快照）")
    private BigDecimal price;

    private Long buyerId;

    private String buyerNickname;

    private String buyerAvatar;

    private Long sellerId;

    private String sellerNickname;

    private String sellerAvatar;

    private String contact;

    private String tradeLocation;

    private String remark;

    private String cancelReason;

    @Schema(description = "0待确认 1已确认 2已完成 3已取消 4已关闭")
    private Integer status;

    @Schema(description = "状态文字")
    private String statusText;

    @Schema(description = "当前用户在这个订单里的身份 BUYER / SELLER")
    private String myRole;

    @Schema(description = "当前用户此刻可以做的操作，由后端算好")
    private OrderActions actions;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    /**
     * 可用操作。
     * <p>
     * 为什么后端算好而不是让前端判断？因为状态机规则（谁能做什么、
     * 在什么状态下能做）是业务知识，应该只有一处实现。
     * 前端照着布尔值渲染按钮即可，规则改了不用动前端。
     */
    @Data
    @Schema(description = "订单可用操作")
    public static class OrderActions implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "卖家确认接单（0→1）")
        private Boolean canConfirm = false;

        @Schema(description = "取消订单（0/1→3）")
        private Boolean canCancel = false;

        @Schema(description = "买家确认完成（1→2）")
        private Boolean canFinish = false;

        @Schema(description = "去商品详情页")
        private Boolean canViewProduct = false;

        @Schema(description = "去留言咨询")
        private Boolean canComment = false;
    }
}
