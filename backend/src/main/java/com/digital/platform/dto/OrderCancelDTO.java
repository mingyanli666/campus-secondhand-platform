package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 取消订单请求。
 * <p>
 * 原因不是必填，但强烈建议填：对面看到订单被取消却不知道为什么，
 * 是最容易产生纠纷的场景。
 */
@Data
@Schema(description = "取消订单请求")
public class OrderCancelDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "取消原因不能超过 255 个字符")
    @Schema(description = "取消原因")
    private String reason;
}
