package com.digital.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改个人资料请求")
public class UpdateProfileDTO {

    @Size(max = 20, message = "昵称不能超过 20 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Min(value = 0, message = "性别参数不合法")
    @Max(value = 2, message = "性别参数不合法")
    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Size(max = 100, message = "学校/区域不能超过 100 个字符")
    @Schema(description = "所在学校/区域")
    private String campus;

    @Size(max = 255, message = "个人简介不能超过 255 个字符")
    @Schema(description = "个人简介")
    private String intro;
}
