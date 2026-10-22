package com.digital.platform.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 商品列表查询条件。
 * <p>
 * 这是"精准筛选"需求的载体：分类、关键词、成色、品牌、价格区间、排序，
 * 组合起来就是数码产品平台的核心检索能力。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品查询条件")
public class ProductQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "关键词，模糊匹配标题、品牌、型号")
    private String keyword;

    @Schema(description = "成色筛选")
    private String conditionLevel;

    @Schema(description = "品牌筛选")
    private String brand;

    @Schema(description = "最低价格")
    private BigDecimal minPrice;

    @Schema(description = "最高价格")
    private BigDecimal maxPrice;

    /**
     * 排序方式，只允许下列固定值（白名单）。
     * 不直接把前端传来的字符串拼进 ORDER BY，是为了防 SQL 注入。
     */
    @Schema(description = "排序：latest最新 / priceAsc价格升序 / priceDesc价格降序 / hot最热")
    private String sort;

    /** 卖家ID。用于"我的发布"，由后端强制赋值，不接受前端传入 */
    @Schema(hidden = true)
    private Long sellerId;

    /** 商品状态。用于"我的发布"按状态筛选 */
    @Schema(description = "状态筛选 0待审核 1已上架 2已下架 3已售出 4审核驳回")
    private Integer status;

    /** 是否需要"仅查询已上架"。公开列表强制为 true */
    @Schema(hidden = true)
    private Boolean onlyOnSale;
}
