package com.digital.platform.controller;

import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.ProductSaveDTO;
import com.digital.platform.security.LoginUser;
import com.digital.platform.security.PublicApi;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.ProductService;
import com.digital.platform.vo.ProductDetailVO;
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

/**
 * 商品接口。
 * <p>
 * 权限设计：
 * - 列表、详情：公开（游客也能浏览）
 * - 发布、编辑、删除、上下架：必须登录，且只能操作自己的商品
 * 归属校验放在 Service 层（requireOwner），因为那是业务规则。
 */
@Tag(name = "05-商品", description = "发布、编辑、检索、详情、上下架")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PublicApi
    @Operation(summary = "商品列表/检索（公开）")
    @GetMapping("/page")
    public Result<PageResult<ProductListVO>> page(ProductQuery query) {
        // 公开列表只允许看到已上架的商品，这个条件由后端强制加上，
        // 不管前端传什么，都不可能出现待审核内容
        query.setSellerId(null);
        query.setStatus(null);
        query.setOnlyOnSale(true);
        return Result.success(productService.page(query));
    }

    @PublicApi
    @Operation(summary = "商品详情（公开，未上架商品仅本人/管理员可见）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        // 未登录时 UserContext.get() 返回 null，是允许的
        LoginUser user = UserContext.get();
        return Result.success(productService.detail(id, user == null ? null : user.getUserId()));
    }

    @Operation(summary = "发布商品")
    @PostMapping
    public Result<Long> publish(@Valid @RequestBody ProductSaveDTO dto) {
        Long id = productService.publish(UserContext.userId(), dto);
        return Result.success("发布成功，等待平台审核", id);
    }

    @Operation(summary = "编辑商品（编辑后需重新审核）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveDTO dto) {
        productService.update(UserContext.userId(), id, dto);
        return Result.success("修改成功，等待平台重新审核", null);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(UserContext.userId(), id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "下架商品")
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.offShelf(UserContext.userId(), id);
        return Result.success("已下架", null);
    }

    @Operation(summary = "重新提交审核")
    @PutMapping("/{id}/resubmit")
    public Result<Void> resubmit(@PathVariable Long id) {
        productService.resubmit(UserContext.userId(), id);
        return Result.success("已提交审核", null);
    }

    @Operation(summary = "我的发布")
    @GetMapping("/mine")
    public Result<PageResult<ProductListVO>> mine(ProductQuery query) {
        // 关键：sellerId 由后端从 token 里取，前端传什么都覆盖掉，
        // 否则改个参数就能看别人的"我的发布"
        query.setSellerId(UserContext.userId());
        query.setOnlyOnSale(false);
        return Result.success(productService.page(query));
    }
}
