package com.digital.platform.controller;

import com.digital.platform.common.result.Result;
import com.digital.platform.dto.LoginDTO;
import com.digital.platform.dto.RegisterDTO;
import com.digital.platform.security.LoginUser;
import com.digital.platform.security.PublicApi;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.UserService;
import com.digital.platform.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口：注册、登录、退出。
 */
@Tag(name = "01-认证", description = "注册、登录、退出登录")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PublicApi
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功", null);
    }

    @PublicApi
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", userService.login(dto));
    }

    /**
     * JWT 是无状态的，服务端没有会话可销毁，
     * 退出登录由前端删除本地 token 完成。这里的接口用于记录日志和后续扩展
     * （例如接入 Redis 做 token 黑名单）。
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        LoginUser user = UserContext.get();
        if (user != null) {
            // 预留：可在此把 token 写入黑名单
        }
        return Result.success();
    }
}
