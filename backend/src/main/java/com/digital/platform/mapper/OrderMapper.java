package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.digital.platform.common.request.OrderQuery;
import com.digital.platform.entity.Order;
import com.digital.platform.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 我的订单列表。
     * <p>
     * 商品标题/封面/价格不需要 JOIN —— 它们在下单时已经快照到订单表里了。
     * 这里 JOIN 用户表只是为了拿到买卖双方的昵称和头像。
     *
     * @param userId 当前登录用户（从 token 取，不是前端传的）
     */
    IPage<OrderVO> selectOrderPage(IPage<OrderVO> page,
                                   @Param("userId") Long userId,
                                   @Param("query") OrderQuery query);
}
