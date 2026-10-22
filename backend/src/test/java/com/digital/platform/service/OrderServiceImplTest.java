package com.digital.platform.service;

import com.digital.platform.common.enums.OrderStatus;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.OrderCreateDTO;
import com.digital.platform.entity.Order;
import com.digital.platform.entity.Product;
import com.digital.platform.mapper.OrderMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 订单状态机与权限校验的单元测试。
 * <p>
 * 为什么优先测订单？因为它同时踩了三个高风险点：
 * <ol>
 *   <li>状态机 —— 6 种状态，每种状态下能做什么都不同</li>
 *   <li>双向权限 —— 买家和卖家能做的事完全相反</li>
 *   <li>资源占用 —— 一件商品不能卖给两个人</li>
 * </ol>
 * 这三类逻辑一旦写错，线上就是真实的交易纠纷，而不是"页面显示不对"。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务 - 状态机与权限")
class OrderServiceImplTest {

    private static final long BUYER = 1L;
    private static final long SELLER = 2L;
    private static final long OTHER = 9L;
    private static final long PRODUCT_ID = 10L;
    private static final long ORDER_ID = 5L;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ============================================================
    //  下单
    // ============================================================

    @Test
    @DisplayName("不能购买自己发布的商品")
    void create_throws_when_buyer_is_product_owner() {
        // 商品 10 的发布者是 BUYER 自己
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, BUYER, ProductStatus.ON_SALE));

        assertBizCode(ResultCode.CANNOT_BUY_SELF,
                () -> orderService.create(BUYER, orderDto(PRODUCT_ID)));
    }

    @Test
    @DisplayName("非在售商品不能下单（待审核 / 已下架 / 已售出）")
    void create_throws_when_product_is_not_on_sale() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, SELLER, ProductStatus.PENDING));

        assertBizCode(ResultCode.PRODUCT_NOT_ON_SALE,
                () -> orderService.create(BUYER, orderDto(PRODUCT_ID)));
    }

    @Test
    @DisplayName("同一商品已有进行中的订单时不能重复下单（一物不二卖）")
    void create_throws_when_an_active_order_already_exists() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, SELLER, ProductStatus.ON_SALE));
        // 已经有一条待确认/已确认的订单
        when(orderMapper.selectCount(any())).thenReturn(1L);

        assertBizCode(ResultCode.PRODUCT_HAS_ACTIVE_ORDER,
                () -> orderService.create(BUYER, orderDto(PRODUCT_ID)));
    }

    // ============================================================
    //  卖家确认
    // ============================================================

    @Test
    @DisplayName("买家不能替卖家确认订单")
    void confirm_throws_when_operator_is_not_seller() {
        when(orderMapper.selectById(ORDER_ID))
                .thenReturn(order(ORDER_ID, BUYER, SELLER, OrderStatus.PENDING));

        assertBizCode(ResultCode.NOT_ORDER_PARTICIPANT,
                () -> orderService.confirm(BUYER, ORDER_ID));
    }

    @Test
    @DisplayName("与订单无关的第三方不能确认订单")
    void confirm_throws_when_operator_is_outsider() {
        when(orderMapper.selectById(ORDER_ID))
                .thenReturn(order(ORDER_ID, BUYER, SELLER, OrderStatus.PENDING));

        assertBizCode(ResultCode.NOT_ORDER_PARTICIPANT,
                () -> orderService.confirm(OTHER, ORDER_ID));
    }

    // ============================================================
    //  买家确认完成
    // ============================================================

    @Test
    @DisplayName("卖家不能单方面把订单标记为完成")
    void finish_throws_when_operator_is_seller() {
        when(orderMapper.selectById(ORDER_ID))
                .thenReturn(order(ORDER_ID, BUYER, SELLER, OrderStatus.CONFIRMED));

        // "东西到手了"只有买家知道，卖家不能替买家宣布交易成功
        assertBizCode(ResultCode.NOT_ORDER_PARTICIPANT,
                () -> orderService.finish(SELLER, ORDER_ID));
    }

    // ============================================================
    //  取消
    // ============================================================

    @Test
    @DisplayName("已完成的订单不能取消（终态不可流转）")
    void cancel_throws_when_order_already_finished() {
        when(orderMapper.selectById(ORDER_ID))
                .thenReturn(order(ORDER_ID, BUYER, SELLER, OrderStatus.FINISHED));

        assertBizCode(ResultCode.ORDER_STATUS_ILLEGAL,
                () -> orderService.cancel(BUYER, ORDER_ID, "不想要了"));
    }

    // ============================================================
    //  辅助方法
    // ============================================================

    /** 断言抛出业务异常，且错误码符合预期 */
    private void assertBizCode(ResultCode expected, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertThat(ex.getCode())
                .as("业务错误码")
                .isEqualTo(expected.getCode());
    }

    private Product product(long id, long ownerId, ProductStatus status) {
        Product p = new Product();
        p.setId(id);
        p.setUserId(ownerId);
        p.setTitle("测试商品");
        p.setPrice(new BigDecimal("100.00"));
        p.setStatus(status.getCode());
        return p;
    }

    private Order order(long id, long buyerId, long sellerId, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setProductId(PRODUCT_ID);
        o.setProductTitle("测试商品");
        o.setBuyerId(buyerId);
        o.setSellerId(sellerId);
        o.setStatus(status.getCode());
        return o;
    }

    private OrderCreateDTO orderDto(long productId) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setProductId(productId);
        dto.setContact("微信 test");
        return dto;
    }
}
