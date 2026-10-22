package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品发布 / 编辑请求。
 * <p>
 * 发布和编辑字段完全一样，所以共用一个 DTO。
 * 编辑时多传一个路径参数 id，由 Controller 区分。
 */
@Data
@Schema(description = "商品发布/编辑请求")
public class ProductSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择商品分类")
    @Schema(description = "分类ID")
    private Long categoryId;

    @NotBlank(message = "请填写商品标题")
    @Size(min = 2, max = 100, message = "标题长度为 2-100 个字符")
    @Schema(description = "标题")
    private String title;

    @NotBlank(message = "请填写商品描述")
    @Size(max = 2000, message = "描述不能超过 2000 个字符")
    @Schema(description = "详细描述")
    private String description;

    @NotNull(message = "请填写出售价格")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @DecimalMax(value = "9999999.99", message = "价格超出合理范围")
    @Schema(description = "出售价格")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "原价不能为负数")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    /* ---------------- 数码专属字段 ---------------- */

    @Size(max = 50, message = "品牌不能超过 50 个字符")
    @Schema(description = "品牌")
    private String brand;

    @Size(max = 100, message = "型号不能超过 100 个字符")
    @Schema(description = "型号")
    private String model;

    @Schema(description = "成色")
    private String conditionLevel;

    @Size(max = 1000, message = "参数配置不能超过 1000 个字符")
    @Schema(description = "参数配置 JSON 文本")
    private String spec;

    @Schema(description = "质保情况")
    private String warranty;

    @Schema(description = "购入时间")
    private LocalDate purchaseDate;

    /* ---------------- 交易信息 ---------------- */

    @Size(max = 100, message = "交易地点不能超过 100 个字符")
    @Schema(description = "交易地点")
    private String tradeLocation;

    @Size(max = 100, message = "联系方式不能超过 100 个字符")
    @Schema(description = "联系方式")
    private String contact;

    /**
     * 图片地址列表。
     * 约定：第一张就是封面图。这样前端只要拖拽排序就能换封面，
     * 不用额外维护一个"哪张是封面"的字段。
     */
    @Size(max = 9, message = "最多上传 9 张图片")
    @Schema(description = "图片地址列表，第一张为封面")
    private List<String> images;
}
