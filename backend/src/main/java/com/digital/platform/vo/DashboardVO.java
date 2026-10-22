package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "管理后台数据看板")
public class DashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户总数")
    private Long userCount;

    @Schema(description = "禁用用户数")
    private Long disabledUserCount;

    @Schema(description = "商品总数（不含已删除）")
    private Long productCount;

    @Schema(description = "待审核商品数")
    private Long pendingCount;

    @Schema(description = "在售商品数")
    private Long onSaleCount;

    @Schema(description = "已售出商品数")
    private Long soldCount;

    @Schema(description = "累计浏览量")
    private Long totalViewCount;
}
