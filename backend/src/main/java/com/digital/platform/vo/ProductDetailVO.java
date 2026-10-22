package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情视图对象。
 * <p>
 * 比列表多出：完整描述、图片列表、参数配置、交易信息、卖家详细信息。
 * 另外带一个 isFavorite 字段，让前端直接知道"当前用户收没收藏"，
 * 不用再单独发一次请求判断。
 */
@Data
@Schema(description = "商品详情")
public class ProductDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "详细描述")
    private String description;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "图片列表")
    private List<String> images;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "成色")
    private String conditionLevel;

    @Schema(description = "参数配置 JSON 文本")
    private String spec;

    @Schema(description = "质保情况")
    private String warranty;

    @Schema(description = "购入时间")
    private LocalDate purchaseDate;

    @Schema(description = "交易地点")
    private String tradeLocation;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "卖家昵称")
    private String sellerNickname;

    @Schema(description = "卖家头像")
    private String sellerAvatar;

    @Schema(description = "卖家信誉分")
    private Integer sellerCreditScore;

    @Schema(description = "卖家所在学校/区域")
    private String sellerCampus;

    @Schema(description = "当前登录用户是否已收藏")
    private Boolean favorite;

    @Schema(description = "当前登录用户是否为发布者本人")
    private Boolean owner;
}
