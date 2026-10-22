package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "强制下架请求")
public class ForceOffShelfDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 注意这里是 @NotBlank 而不是可选。
     * 强制下架是"平台对用户内容的处置"，必须说明理由：
     * 一方面要留痕（追究责任时有据可查），
     * 另一方面卖家有权知道自己的商品为什么被下架。
     */
    @NotBlank(message = "请填写下架原因")
    @Size(max = 255, message = "下架原因不能超过 255 个字符")
    @Schema(description = "下架原因")
    private String reason;
}
