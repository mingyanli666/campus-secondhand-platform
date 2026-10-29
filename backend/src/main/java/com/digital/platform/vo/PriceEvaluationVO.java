package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 价格评估结果（拓展接口的返回体）。
 * <p>
 * 字段是为「基线算法」和「未来的模型」共同设计的：
 * 不管底层是算术平均还是回归模型，用户看到的永远是
 * 「同类多少钱 / 你这个贵不贵 / 建议是什么」这三件事。
 */
@Data
@Schema(description = "价格评估结果")
public class PriceEvaluationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前标价")
    private BigDecimal currentPrice;

    @Schema(description = "同类商品平均价")
    private BigDecimal averagePrice;

    @Schema(description = "同类商品最低价")
    private BigDecimal minPrice;

    @Schema(description = "同类商品最高价")
    private BigDecimal maxPrice;

    @Schema(description = "参考样本数量")
    private Integer sampleCount;

    @Schema(description = "结论：偏低 / 合理 / 偏高 / 数据不足")
    private String level;

    @Schema(description = "一句话说明")
    private String advice;
}
