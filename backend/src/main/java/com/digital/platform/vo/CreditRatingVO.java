package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户信誉评级（拓展接口的返回体）。
 * <p>
 * 除了总分和等级，还**把每个维度单独返回**：
 * 用户看到"较差"时应该知道是"成交太少"还是"被举报过"，
 * 只给一个数字等于让人猜。
 */
@Data
@Schema(description = "用户信誉评级")
public class CreditRatingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "信誉得分 0-100")
    private Integer score;

    @Schema(description = "等级：优秀 / 良好 / 一般 / 较差")
    private String level;

    @Schema(description = "已完成成交数（作为卖家）")
    private Long soldCount;

    @Schema(description = "发布商品数")
    private Long productCount;

    @Schema(description = "被举报且成立的次数")
    private Long reportCount;

    @Schema(description = "一句话说明")
    private String description;
}
