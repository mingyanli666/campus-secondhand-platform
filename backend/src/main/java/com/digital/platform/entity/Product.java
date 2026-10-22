package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 二手数码商品实体，对应 biz_product 表。
 * <p>
 * 带 ★ 的字段是"数码产品专属"字段，也是本项目区别于通用二手平台的关键：
 * 手机要看内存/存储/成色/是否在保，相机要看快门数，笔记本要看配置。
 */
@Data
@TableName("biz_product")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者ID */
    private Long userId;

    private Long categoryId;

    private String title;

    private String description;

    /** 出售价格。用 BigDecimal 不用 double —— double 有精度误差，钱不能用 */
    private BigDecimal price;

    /** 原价，用于展示"立省 xxx" */
    private BigDecimal originalPrice;

    private String coverImage;

    // ---------------- 数码专属字段 ----------------

    /** ★ 品牌，如 Apple */
    private String brand;

    /** ★ 型号，如 iPhone 14 Pro */
    private String model;

    /** ★ 成色：全新 / 99新 / 95新 / 9成新 / 8成新 / 7成新及以下 */
    private String conditionLevel;

    /** ★ 参数配置，存 JSON 字符串，如 {"内存":"8G","存储":"256G"}，键值对形态可灵活适应各类数码产品 */
    private String spec;

    /** ★ 质保情况：在保 / 过保 / 无质保 */
    private String warranty;

    /** ★ 购入时间，买家据此判断剩余保修期 */
    private LocalDate purchaseDate;

    /** 交易地点 */
    private String tradeLocation;

    /** 联系方式 */
    private String contact;

    // ---------------- 状态与统计 ----------------

    /** 0待审核 1已上架 2已下架 3已售出 4审核驳回 */
    private Integer status;

    /**
     * 平台处置原因。
     * 审核驳回时填"为什么不合格"，管理员强制下架时填"为什么被下架"。
     * 两个场景共用同一列，是为了避免为低频场景单独加字段；语义由 status 区分。
     */
    private String rejectReason;

    /**
     * 浏览量。
     * 更新时用 "view_count = view_count + 1" 的原子写法，
     * 不能先查出来再 +1 存回去（并发下会丢计数）。
     */
    private Integer viewCount;

    private Integer favoriteCount;

    /** 审核通过的上架时间 */
    private LocalDateTime publishTime;

    private LocalDateTime auditTime;

    private Long auditorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
