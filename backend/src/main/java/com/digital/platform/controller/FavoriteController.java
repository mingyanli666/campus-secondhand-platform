package com.digital.platform.controller;

import com.digital.platform.common.request.PageQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.FavoriteService;
import com.digital.platform.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏接口。
 * <p>
 * 所有操作的都是"当前登录用户自己的收藏"，userId 从 token 取，
 * 前端无法查看或修改别人的收藏夹。
 */
@Tag(name = "11-收藏", description = "收藏、取消收藏、我的收藏")
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏商品")
    @PostMapping("/{productId}")
    public Result<Void> add(@PathVariable Long productId) {
        favoriteService.add(UserContext.userId(), productId);
        return Result.success("收藏成功", null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.remove(UserContext.userId(), productId);
        return Result.success("已取消收藏", null);
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/page")
    public Result<PageResult<ProductListVO>> page(PageQuery query) {
        return Result.success(favoriteService.pageMine(UserContext.userId(), query));
    }
}
