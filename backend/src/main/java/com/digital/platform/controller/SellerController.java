package com.digital.platform.controller;

import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.security.PublicApi;
import com.digital.platform.service.ProductService;
import com.digital.platform.service.UserService;
import com.digital.platform.vo.ProductListVO;
import com.digital.platform.vo.UserPublicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家主页接口。
 * <p>
 * 场景：买家在商品详情页看到「张三」，点进去看这个人还发布了什么 ——
 * 很可能他还有别的闲置，一次逛完比来回搜更省事。
 * <p>
 * 两个接口都是公开的：游客也应该能看卖家主页，否则还得先注册才能判断
 * 这个卖家靠不靠谱。
 */
@Tag(name = "16-卖家主页", description = "卖家公开资料、在售商品")
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final UserService userService;
    private final ProductService productService;

    @PublicApi
    @Operation(summary = "卖家公开资料（含在售/已售数量）")
    @GetMapping("/{id}")
    public Result<UserPublicVO> profile(@PathVariable Long id) {
        return Result.success(userService.getPublicProfile(id));
    }

    /**
     * 卖家的在售商品。
     * <p>
     * 复用商品列表的查询和 SQL —— 只是强制加上 sellerId 和"只在售"两个条件。
     * 这样品牌、价格区间这些筛选能力也顺带有了，不用重写一遍。
     */
    @PublicApi
    @Operation(summary = "卖家在售商品列表")
    @GetMapping("/{id}/products")
    public Result<PageResult<ProductListVO>> products(@PathVariable Long id, ProductQuery query) {
        // 下面三个条件由后端强制覆盖，前端传什么都不算数：
        // 防止有人用 ?sellerId=别人 或 ?status=0 把待审核商品翻出来
        query.setSellerId(id);
        query.setOnlyOnSale(true);
        query.setStatus(null);
        return Result.success(productService.page(query));
    }
}
