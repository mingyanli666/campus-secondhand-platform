package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "请输入账号")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "账号为 4-20 位字母、数字或下划线")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "请输入昵称")
    @Size(max = 20, message = "昵称不能超过 20 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度为 6-20 个字符")
    @Schema(description = "登录密码")
    private String password;
}
