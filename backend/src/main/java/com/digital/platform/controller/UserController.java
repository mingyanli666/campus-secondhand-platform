package com.digital.platform.controller;

import com.digital.platform.common.result.Result;
import com.digital.platform.dto.ChangePasswordDTO;
import com.digital.platform.dto.UpdateProfileDTO;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.UserService;
import com.digital.platform.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人信息管理接口。
 * <p>
 * 所有接口操作的都是"当前登录用户"，用户 ID 从 token 解析而来，
 * 前端无法通过传参修改别人的资料，从设计上杜绝越权。
 */
@Tag(name = "02-个人信息", description = "查看/修改资料、修改密码、上传头像")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户资料")
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getProfile(UserContext.userId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success("资料已更新", userService.updateProfile(UserContext.userId(), dto));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.userId(), dto);
        return Result.success("密码修改成功，请重新登录", null);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success("头像上传成功", userService.uploadAvatar(UserContext.userId(), file));
    }
}
