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
@Schema(description = "举报商品请求")
public class ReportCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "缺少商品ID")
    @Schema(description = "商品ID")
    private Long productId;

    @NotNull(message = "请选择举报类型")
    @Min(value = 1, message = "举报类型不合法")
    @Max(value = 4, message = "举报类型不合法")
    @Schema(description = "1虚假信息 2违禁物品 3价格欺诈 4其他")
    private Integer type;

    @NotBlank(message = "请说明举报理由")
    @Size(min = 5, max = 255, message = "举报理由为 5-255 个字符")
    @Schema(description = "举报说明")
    private String reason;
}
