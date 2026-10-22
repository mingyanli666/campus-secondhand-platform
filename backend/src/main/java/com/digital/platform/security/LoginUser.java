package com.digital.platform.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户上下文对象，从 token 解析而来，随请求生命周期存在。
 * 只放必要字段，不放密码等敏感信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前登录用户")
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

    /** USER / ADMIN */
    private String role;
}
