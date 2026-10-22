package com.digital.platform.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.enums.NotificationType;
import com.digital.platform.common.enums.OrderStatus;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.OrderQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.OrderCreateDTO;
import com.digital.platform.entity.Order;
import com.digital.platform.entity.Product;
import com.digital.platform.mapper.OrderMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.NotificationService;
import com.digital.platform.service.OrderService;
import com.digital.platform.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final NotificationService notificationService;

    // ============================================================
    //  下单
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long buyerId, OrderCreateDTO dto) {
        Product product = requireProduct(dto.getProductId());

        if (product.getUserId().equals(buyerId)) {
            throw new BusinessException(ResultCode.CANNOT_BUY_SELF);
        }
        if (!ProductStatus.ON_SALE.getCode().equals(product.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_ON_SALE,
                    "该商品当前状态为「" + ProductStatus.descOf(product.getStatus()) + "」，无法下单");
        }

        // 二手商品是"一件"，不能同时卖给两个人。
        // 只要已经有一个待确认或已确认的订单，就不允许再下单。
        // 注意这里是"事后检查"，理论上存在并发窗口（两个人同时通过检查），
        // 但下单是低频操作、影响也仅仅是卖家收到两个订单，他挑一个确认就行。
        // 真要做到严格互斥，需要给商品加乐观锁或分布式锁 —— 对本项目属于过度设计。
        Long activeCount = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getProductId, product.getId())
                .in(Order::getStatus, OrderStatus.activeCodes()));
        if (activeCount > 0) {
            throw new BusinessException(ResultCode.PRODUCT_HAS_ACTIVE_ORDER);
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setProductId(product.getId());
        // ↓ 三个快照：商品以后被改了，订单里显示的仍是成交当时的样子
        order.setProductTitle(product.getTitle());
        order.setProductCover(product.getCoverImage());
        order.setPrice(product.getPrice());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setContact(dto.getContact());
        // 买家没填就沿用商品里卖家写的交易地点，通常就是同一个地方
        order.setTradeLocation(StrUtil.blankToDefault(dto.getTradeLocation(), product.getTradeLocation()));
        order.setRemark(dto.getRemark());
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // order_no 上的唯一索引撞了。概率极低，但撞上时给用户一个明确的提示，
            // 而不是抛出一句看不懂的数据库错误
            throw new BusinessException("订单号生成冲突，请重试一次");
        }

        notificationService.send(product.getUserId(), NotificationType.ORDER,
                "你有一笔新的订单",
                "有买家想买你的「" + product.getTitle() + "」。请尽快确认或取消，避免买家久等。",
                order.getId());

        log.info("用户 {} 对商品 {} 下单，订单号 {}", buyerId, product.getId(), order.getOrderNo());
        return order.getId();
    }

    // ============================================================
    //  状态流转
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long userId, Long orderId) {
        Order order = requireOrder(orderId);

        // 只有卖家能确认接单
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_ORDER_PARTICIPANT, "只有卖家可以确认订单");
        }
        requireStatus(order, OrderStatus.PENDING, "确认");

        changeStatus(orderId, OrderStatus.CONFIRMED, null, false);

        notificationService.send(order.getBuyerId(), NotificationType.ORDER,
                "卖家已确认订单",
                "你购买「" + order.getProductTitle() + "」的订单已被卖家确认。"
                        + "请按约定联系卖家完成交易，交易完成后记得点「确认完成」。",
                orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId, String reason) {
        Order order = requireOrder(orderId);

        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ResultCode.NOT_ORDER_PARTICIPANT);
        }

        // 已完成 / 已取消 / 已关闭 都是终态，不能再取消
        if (!OrderStatus.activeCodes().contains(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL,
                    "该订单当前状态为「" + OrderStatus.descOf(order.getStatus()) + "」，无法取消");
        }

        changeStatus(orderId, OrderStatus.CANCELED, StrUtil.blankToDefault(reason, "未填写原因"), false);

        // 通知对方 —— 让谁取消的、为什么取消都写清楚，
        // 否则对面只看到"订单没了"，很容易产生纠纷
        Long notifyUser = isBuyer ? order.getSellerId() : order.getBuyerId();
        String who = isBuyer ? "买家" : "卖家";
        notificationService.send(notifyUser, NotificationType.ORDER,
                "订单已取消",
                who + "取消了「" + order.getProductTitle() + "」的订单。原因："
                        + StrUtil.blankToDefault(reason, "未填写原因"),
                orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long userId, Long orderId) {
        Order order = requireOrder(orderId);

        // 只有买家能确认完成。
        // 为什么不让卖家也能点？因为"东西到手了"这件事只有买家知道，
        // 让卖家能单方面结束订单，等于给了他一键把交易标记为完成的权力。
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_ORDER_PARTICIPANT, "只有买家可以确认完成");
        }
        requireStatus(order, OrderStatus.CONFIRMED, "标记完成");

        changeStatus(orderId, OrderStatus.FINISHED, null, true);

        // 交易完成 → 商品标记为已售出，从公开列表里消失
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getStatus, ProductStatus.SOLD.getCode())
                .set(Product::getUpdateTime, LocalDateTime.now())
                .eq(Product::getId, order.getProductId()));

        notificationService.send(order.getSellerId(), NotificationType.ORDER,
                "订单已完成",
                "「" + order.getProductTitle() + "」的交易已完成，商品已标记为售出。"
                        + "感谢你使用本平台。",
                orderId);
    }

    // ============================================================
    //  查询
    // ============================================================

    @Override
    public PageResult<OrderVO> pageMine(Long userId, OrderQuery query) {
        Page<OrderVO> page = query.toPage();
        IPage<OrderVO> result = orderMapper.selectOrderPage(page, userId, query);
        // 状态文字、我的身份、可用操作都由后端算好 —— 状态机规则只应该有一处实现
        result.getRecords().forEach(vo -> fillView(vo, userId));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeActiveOrdersByProduct(Long productId, String reason) {
        List<Order> actives = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getProductId, productId)
                .in(Order::getStatus, OrderStatus.activeCodes()));
        if (actives.isEmpty()) {
            return;
        }

        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, OrderStatus.CLOSED.getCode())
                .set(Order::getCancelReason, StrUtil.blankToDefault(reason, "商品已被平台下架"))
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getProductId, productId)
                .in(Order::getStatus, OrderStatus.activeCodes()));

        // 通知买卖双方。商品下架是平台行为，两边都该知道，
        // 否则他们还在等着跟对方交易
        for (Order o : actives) {
            String content = "「" + o.getProductTitle() + "」已被平台下架，相关订单已自动关闭。原因："
                    + StrUtil.blankToDefault(reason, "商品已被平台下架");
            notificationService.send(o.getBuyerId(), NotificationType.ORDER, "订单已关闭", content, o.getId());
            notificationService.send(o.getSellerId(), NotificationType.ORDER, "订单已关闭", content, o.getId());
        }
    }

    // ============================================================
    //  私有辅助
    // ============================================================

    private String generateOrderNo() {
        // 格式：OD + 年月日时分秒毫秒 + 4 位随机数
        // 时间戳保证基本有序（方便按号排序排查），随机数避免同一毫秒内碰撞
        return "OD" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(4);
    }

    private void changeStatus(Long orderId, OrderStatus to, String cancelReason, boolean markFinish) {
        LocalDateTime now = LocalDateTime.now();
        // 用 LambdaUpdateWrapper 而不是 updateById，因为要能把 cancelReason 写成 null
        // （updateById 会跳过 null 字段，见 docs/04 第四部分）
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, to.getCode())
                .set(Order::getCancelReason, cancelReason)
                .set(markFinish, Order::getFinishTime, now)
                .set(Order::getUpdateTime, now)
                .eq(Order::getId, orderId));
    }

    private void requireStatus(Order order, OrderStatus expected, String action) {
        if (!expected.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL,
                    "当前状态为「" + OrderStatus.descOf(order.getStatus()) + "」，无法" + action);
        }
    }

    /**
     * 把状态字段翻译成给前端看的内容。
     * 关键是 actions —— 前端不做任何状态机判断，照布尔值渲染按钮就行。
     */
    private void fillView(OrderVO vo, Long userId) {
        vo.setStatusText(OrderStatus.descOf(vo.getStatus()));

        boolean isBuyer = vo.getBuyerId().equals(userId);
        vo.setMyRole(isBuyer ? "BUYER" : "SELLER");

        OrderVO.OrderActions actions = new OrderVO.OrderActions();
        actions.setCanViewProduct(vo.getProductId() != null);
        actions.setCanComment(true);

        Integer status = vo.getStatus();
        if (OrderStatus.PENDING.getCode().equals(status)) {
            // 待确认：卖家能确认，两边都能取消
            actions.setCanConfirm(!isBuyer);
            actions.setCanCancel(true);
        } else if (OrderStatus.CONFIRMED.getCode().equals(status)) {
            // 已确认：买家能确认完成，两边都能取消
            actions.setCanFinish(isBuyer);
            actions.setCanCancel(true);
        }
        vo.setActions(actions);
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }
}
