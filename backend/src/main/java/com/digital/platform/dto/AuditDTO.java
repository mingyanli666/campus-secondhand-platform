package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "商品审核请求")
public class AuditDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择审核结果")
    @Min(value = 1, message = "审核动作不合法")
    @Max(value = 2, message = "审核动作不合法")
    @Schema(description = "审核动作 1通过 2驳回")
    private Integer action;

    /**
     * 驳回原因。
     * 这里不能加 @NotBlank —— 因为"通过"时不需要填原因。
     * 这种"根据另一个字段决定是否必填"的校验叫条件校验，
     * 用注解表达不了，放在 Service 层判断。
     */
    @Size(max = 255, message = "驳回原因不能超过 255 个字符")
    @Schema(description = "驳回原因，action=2 时必填")
    private String reason;
}
