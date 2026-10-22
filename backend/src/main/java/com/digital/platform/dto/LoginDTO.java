package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = "请输入账号")
    @Size(min = 4, max = 20, message = "账号长度为 4-20 个字符")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度为 6-20 个字符")
    @Schema(description = "登录密码")
    private String password;
}
