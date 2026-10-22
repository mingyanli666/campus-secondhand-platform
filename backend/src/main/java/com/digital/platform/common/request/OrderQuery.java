package com.digital.platform.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 订单查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单查询条件")
public class OrderQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 视角：BUY=我买到的（我是买家），SELL=我卖出的（我是卖家）。
     * 由后端根据前端传的值决定加 buyer_id 还是 seller_id 条件，
     * 但**具体是哪个人**始终从 token 取，前端只能选视角、不能指定人。
     */
    @Schema(description = "视角 BUY我买到的 / SELL我卖出的")
    private String role;

    @Schema(description = "状态筛选 0待确认 1已确认 2已完成 3已取消 4已关闭")
    private Integer status;
}
