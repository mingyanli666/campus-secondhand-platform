package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应 biz_order 表。
 * <p>
 * 注意 product_title / product_cover / price 三个字段存的是**下单那一刻的快照**。
 * 为什么不像其他表那样只存 product_id 然后 JOIN？
 * <p>
 * 因为订单是"交易凭证"。商品后来被卖家改了标题、换了图片、改了价格，
 * 订单里显示的必须还是**当时约定的内容**，否则买卖双方对账时各说各话。
 * 这和审核记录表存 product_title 快照是同一个道理 —— 见
 * docs/04 第五部分"快照式审计表设计"。
 */
@Data
@TableName("biz_order")
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，对外展示用，用户报问题时说这个比说 id 清楚 */
    private String orderNo;

    private Long productId;

    /** 商品标题快照 */
    private String productTitle;

    /** 商品封面快照 */
    private String productCover;

    private Long buyerId;

    private Long sellerId;

    /** 成交价快照 */
    private BigDecimal price;

    /** 买家联系方式 */
    private String contact;

    /** 约定交易地点 */
    private String tradeLocation;

    /** 买家留言 */
    private String remark;

    /** 取消 / 关闭原因 */
    private String cancelReason;

    /** 0待确认 1已确认 2已完成 3已取消 4已关闭 */
    private Integer status;

    /** 成交完成时间 */
    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
