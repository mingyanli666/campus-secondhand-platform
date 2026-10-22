package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用的"启用/停用"请求体。
 * <p>
 * 用户和分类的状态语义完全一致（1 启用 / 0 禁用），所以共用一个 DTO。
 * 一开始我把它命名成 UserStatusDTO，加到分类接口上时名字就说不通了 ——
 * 命名要按"它是什么"而不是"第一次用在哪"。
 */
@Data
@Schema(description = "状态变更请求")
public class StatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择状态")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    @Schema(description = "1 启用 0 禁用")
    private Integer status;
}
