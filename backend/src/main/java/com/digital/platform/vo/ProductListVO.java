package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项视图对象。
 * <p>
 * 刻意不含 description（描述可能很长），列表页用不到却会显著增加传输体积。
 * 这就是"列表接口"和"详情接口"分开的原因 —— 只传前端需要的字段。
 */
@Data
@Schema(description = "商品列表项")
public class ProductListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "成色")
    private String conditionLevel;

    @Schema(description = "质保情况")
    private String warranty;

    @Schema(description = "状态 0待审核 1已上架 2已下架 3已售出 4审核驳回")
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
}
