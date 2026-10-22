package com.digital.platform.service;

import com.digital.platform.common.request.OrderQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.dto.OrderCreateDTO;
import com.digital.platform.vo.OrderVO;

public interface OrderService {

    /** 下单，返回订单ID */
    Long create(Long buyerId, OrderCreateDTO dto);

    /** 卖家确认接单（0 待确认 → 1 已确认） */
    void confirm(Long userId, Long orderId);

    /** 取消订单（0/1 → 3 已取消），买卖双方都可以操作 */
    void cancel(Long userId, Long orderId, String reason);

    /** 买家确认完成（1 已确认 → 2 已完成），同时把商品标记为已售出 */
    void finish(Long userId, Long orderId);

    /** 我的订单（role 决定视角：我买到的 / 我卖出的） */
    PageResult<OrderVO> pageMine(Long userId, OrderQuery query);

    /**
     * 关闭某个商品下所有进行中的订单。
     * 管理员强制下架或删除商品时调用 —— 商品都没了，订单不能再挂着。
     */
    void closeActiveOrdersByProduct(Long productId, String reason);
}
