package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "下单请求")
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "缺少商品ID")
    @Schema(description = "商品ID")
    private Long productId;

    @NotBlank(message = "请填写联系方式")
    @Size(max = 100, message = "联系方式不能超过 100 个字符")
    @Schema(description = "联系方式，卖家要用它联系你")
    private String contact;

    @Size(max = 100, message = "交易地点不能超过 100 个字符")
    @Schema(description = "希望的交易地点")
    private String tradeLocation;

    @Size(max = 255, message = "留言不能超过 255 个字符")
    @Schema(description = "给卖家的留言，如方便交易的时间")
    private String remark;
}
