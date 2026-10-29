package com.digital.platform.controller;

import com.digital.platform.common.result.Result;
import com.digital.platform.extension.CreditRatingService;
import com.digital.platform.extension.PriceEvaluationService;
import com.digital.platform.extension.RecommendService;
import com.digital.platform.security.PublicApi;
import com.digital.platform.vo.CreditRatingVO;
import com.digital.platform.vo.PriceEvaluationVO;
import com.digital.platform.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 拓展功能接口（推荐 / 价格评估 / 信誉评级）。
 * <p>
 * <b>这三个接口存在的意义</b>：需求里的"可拓展性良好"如果没有实物支撑，
 * 就只是一句空话。这里把三个后续方向落成可调用、可演示的接口 ——
 * 现在是基线算法，换实现类即可升级，<b>本 Controller 一行都不用改</b>。
 *
 * <p>
 * 全部标 {@link PublicApi}：这几个都是给浏览者看的辅助信息，
 * 游客在商品详情页也应该能看到"同类价格参考"和"看了又看"。
 */
@Tag(name = "08-拓展功能", description = "智能推荐、价格评估、信誉评级（基线实现，接口已稳定）")
@RestController
@RequestMapping("/api/extension")
@RequiredArgsConstructor
public class ExtensionController {

    private final RecommendService recommendService;
    private final PriceEvaluationService priceEvaluationService;
    private final CreditRatingService creditRatingService;

    @PublicApi
    @Operation(summary = "相似商品推荐（详情页「看了又看」）")
    @GetMapping("/recommend/similar/{productId}")
    public Result<List<ProductListVO>> similar(@PathVariable Long productId,
                                               @RequestParam(defaultValue = "4") int limit) {
        return Result.success(recommendService.similarProducts(productId, limit));
    }

    @PublicApi
    @Operation(summary = "热门商品（首页推荐位）")
    @GetMapping("/recommend/hot")
    public Result<List<ProductListVO>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(recommendService.hotProducts(limit));
    }

    @PublicApi
    @Operation(summary = "价格评估：当前标价相对同类商品是偏高还是偏低")
    @GetMapping("/price-evaluation/{productId}")
    public Result<PriceEvaluationVO> priceEvaluation(@PathVariable Long productId) {
        return Result.success(priceEvaluationService.evaluate(productId));
    }

    @PublicApi
    @Operation(summary = "用户信誉评级")
    @GetMapping("/credit/{userId}")
    public Result<CreditRatingVO> credit(@PathVariable Long userId) {
        return Result.success(creditRatingService.rating(userId));
    }
}
