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
@Schema(description = "举报处理请求")
public class ReportHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择处理结果")
    @Min(value = 1, message = "处理结果不合法")
    @Max(value = 2, message = "处理结果不合法")
    @Schema(description = "1 已处理（举报成立）2 已驳回（举报不成立）")
    private Integer status;

    /**
     * 处理结论。必填，因为它会原样通知给举报人 ——
     * 只说"已处理"却不说什么结论，举报人等于没收到反馈。
     */
    @NotBlank(message = "请填写处理结论")
    @Size(max = 255, message = "处理结论不能超过 255 个字符")
    @Schema(description = "处理结论，会通知给举报人")
    private String handleResult;

    /**
     * 是否同时把商品强制下架。
     * <p>
     * 只在"举报成立"时有意义。这样一个举报处理动作就能同时完成
     * "记录结论 + 处置商品 + 通知双方"，不用管理员再去商品管理里操作一遍。
     */
    @Schema(description = "举报成立时是否同时下架商品")
    private Boolean offShelf = Boolean.FALSE;
}
