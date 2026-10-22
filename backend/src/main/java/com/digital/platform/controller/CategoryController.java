package com.digital.platform.controller;

import com.digital.platform.common.result.Result;
import com.digital.platform.security.PublicApi;
import com.digital.platform.service.CategoryService;
import com.digital.platform.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "03-商品分类", description = "分类查询")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分类列表是纯字典数据，不需要登录即可访问，所以标 @PublicApi。
     * 首页、发布页、搜索页都要用它。
     */
    @PublicApi
    @Operation(summary = "查询启用的分类列表")
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
