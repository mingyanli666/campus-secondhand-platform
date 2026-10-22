package com.digital.platform.controller;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.request.AdminUserQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.StatusDTO;
import com.digital.platform.security.RequireRole;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.AdminUserService;
import com.digital.platform.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 - 用户管理接口。
 * <p>
 * 这里刻意**没有提供"修改用户角色"的接口**。给用户提权是一件需要谨慎对待的事，
 * 一个点的位置不对就可能把自己或别人变成管理员。本项目不做角色变更，
 * 管理员账号由 DataInitializer 初始化，需要新增就走数据库 —— 这是有意的收紧。
 */
@Tag(name = "09-管理后台·用户管理", description = "用户查询、启用禁用、重置密码")
@RestController
@RequestMapping("/api/admin/user")
@RequireRole(RoleType.ADMIN)
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表")
    @GetMapping("/page")
    public Result<PageResult<AdminUserVO>> page(AdminUserQuery query) {
        return Result.success(adminUserService.page(query));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adminUserService.updateStatus(UserContext.userId(), id, dto.getStatus());
        return Result.success(dto.getStatus() == 1 ? "已启用该账号" : "已禁用该账号", null);
    }

    /**
     * 重置密码。
     * 新密码只在这一次响应里返回，不入库、不写日志，管理员需要当场转告用户。
     */
    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id) {
        String newPassword = adminUserService.resetPassword(id);
        return Result.success("密码已重置，请把新密码告知该用户", newPassword);
    }
}
