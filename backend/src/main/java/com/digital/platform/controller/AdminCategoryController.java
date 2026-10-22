package com.digital.platform.controller;

import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.CategorySaveDTO;
import com.digital.platform.dto.StatusDTO;
import com.digital.platform.security.RequireRole;
import com.digital.platform.service.AdminCategoryService;
import com.digital.platform.vo.AdminCategoryVO;
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

@Tag(name = "10-管理后台·分类维护", description = "分类增删改查、排序、启停")
@RestController
@RequestMapping("/api/admin/category")
@RequireRole(RoleType.ADMIN)
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @Operation(summary = "分类列表（含停用）")
    @GetMapping("/list")
    public Result<List<AdminCategoryVO>> list() {
        return Result.success(adminCategoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategorySaveDTO dto) {
        return Result.success("新增成功", adminCategoryService.create(dto));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        adminCategoryService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用分类")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        adminCategoryService.updateStatus(id, dto.getStatus());
        return Result.success(dto.getStatus() == 1 ? "已启用" : "已停用", null);
    }
}
