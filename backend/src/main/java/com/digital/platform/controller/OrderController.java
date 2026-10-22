package com.digital.platform.controller;

import com.digital.platform.common.request.OrderQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.Result;
import com.digital.platform.dto.OrderCancelDTO;
import com.digital.platform.dto.OrderCreateDTO;
import com.digital.platform.security.UserContext;
import com.digital.platform.service.OrderService;
import com.digital.platform.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 * <p>
 * 所有接口都不接受"用户ID"作为参数 —— 一律从 token 取。
 * 前端能传的只有"商品ID"和"订单ID"，而这两个都会在 Service 层校验归属，
 * 所以改参数也操作不了别人的订单。
 */
@Tag(name = "13-订单", description = "下单、确认、取消、完成、我的订单")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrderCreateDTO dto) {
        Long id = orderService.create(UserContext.userId(), dto);
        return Result.success("下单成功，等待卖家确认", id);
    }

    @Operation(summary = "我的订单（role=BUY我买到的 / SELL我卖出的）")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(OrderQuery query) {
        return Result.success(orderService.pageMine(UserContext.userId(), query));
    }

    @Operation(summary = "卖家确认订单")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(UserContext.userId(), id);
        return Result.success("已确认订单，请与买家联系完成交易", null);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody OrderCancelDTO dto) {
        orderService.cancel(UserContext.userId(), id, dto.getReason());
        return Result.success("订单已取消", null);
    }

    @Operation(summary = "买家确认完成")
    @PutMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id) {
        orderService.finish(UserContext.userId(), id);
        return Result.success("交易已完成，商品已标记为售出", null);
    }
}
