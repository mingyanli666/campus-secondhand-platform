package com.digital.platform.controller;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.AuditDTO;
import com.digital.platform.dto.ForceOffShelfDTO;
import com.digital.platform.security.LoginUser;
import com.digital.platform.security.RequireRole;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.AdminProductService;
import com.digital.platform.vo.AuditRecordVO;
import com.digital.platform.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理后台 - 商品审核接口。
 * <p>
 * 类上加 {@code @RequireRole(RoleType.ADMIN)}：拦截器会校验当前登录用户的角色，
 * 普通用户即使带着完全合法的 token 来调这些接口，也会被拦到 403。
 * 这是"严格区分普通用户与平台管理员权限、杜绝越权操作"的核心实现。
 */
@Tag(name = "06-管理后台·商品审核", description = "商品审核、强制下架、审核留痕")
@RestController
@RequestMapping("/api/admin/product")
@RequireRole(RoleType.ADMIN)
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "商品列表（含所有状态，可按状态筛选）")
    @GetMapping("/page")
    public Result<PageResult<ProductListVO>> page(ProductQuery query) {
        return Result.success(adminProductService.page(query));
    }

    @Operation(summary = "审核商品（通过/驳回）")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        LoginUser admin = UserContext.require();
        adminProductService.audit(admin.getUserId(), admin.getUsername(), id, dto);
        return Result.success(dto.getAction() == 1 ? "已通过审核，商品已上架" : "已驳回，已通知卖家", null);
    }

    @Operation(summary = "强制下架商品")
    @PutMapping("/{id}/off-shelf")
    public Result<Void> forceOffShelf(@PathVariable Long id,
                                      @Valid @RequestBody ForceOffShelfDTO dto) {
        LoginUser admin = UserContext.require();
        adminProductService.forceOffShelf(admin.getUserId(), admin.getUsername(), id, dto);
        return Result.success("已强制下架，并已通知卖家", null);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminProductService.delete(id);
        return Result.success("已删除", null);
    }

    @Operation(summary = "查询商品的审核历史")
    @GetMapping("/{id}/audit-records")
    public Result<List<AuditRecordVO>> auditRecords(@PathVariable Long id) {
        return Result.success(adminProductService.auditRecords(id));
    }
}
