package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "发布留言/回复请求")
public class CommentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "缺少商品ID")
    @Schema(description = "商品ID")
    private Long productId;

    @NotBlank(message = "请填写留言内容")
    @Size(max = 500, message = "留言内容不能超过 500 个字符")
    @Schema(description = "留言内容")
    private String content;

    /**
     * 父留言 ID。为空表示对商品的一级留言（咨询），有值表示回复某条留言。
     * 前端不传就是咨询，传了就是回复，不用两个接口。
     */
    @Schema(description = "父留言ID，为空表示一级留言")
    private Long parentId;
}
