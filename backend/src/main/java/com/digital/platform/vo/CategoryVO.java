package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类视图对象。分类本身没有敏感信息，但用 VO 可以过滤掉
 * deleted、createTime 这类前端用不到的字段，让接口返回更干净。
 */
@Data
@Schema(description = "商品分类")
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标")
    private String icon;

    @Schema(description = "排序值")
    private Integer sort;
}
