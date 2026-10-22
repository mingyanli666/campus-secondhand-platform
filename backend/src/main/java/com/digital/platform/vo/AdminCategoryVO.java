package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 管理后台 - 分类列表项。
 * 比前台多一个 productCount：管理员想删分类前，得先知道这个分类下有多少商品，
 * 否则删完发现一堆商品变成了"未分类"。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台分类信息")
public class AdminCategoryVO extends CategoryVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "该分类下的商品数")
    private Long productCount;
}
