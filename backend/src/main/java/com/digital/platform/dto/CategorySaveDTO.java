package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "分类新增/编辑请求")
public class CategorySaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请填写分类名称")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    @Schema(description = "分类名称")
    private String name;

    @Size(max = 255, message = "图标名不能超过 255 个字符")
    @Schema(description = "图标名称，必须是 Element Plus 提供的图标名")
    private String icon;

    @NotNull(message = "请填写排序值")
    @Min(value = 0, message = "排序值不能为负数")
    @Max(value = 9999, message = "排序值超出范围")
    @Schema(description = "排序值，越小越靠前")
    private Integer sort;

    @NotNull(message = "请选择状态")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    @Schema(description = "1 启用 0 停用")
    private Integer status;
}
