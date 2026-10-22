package com.digital.platform.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "举报查询条件")
public class ReportQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态筛选 0待处理 1已处理 2已驳回")
    private Integer status;

    @Schema(description = "类型筛选 1虚假信息 2违禁物品 3价格欺诈 4其他")
    private Integer type;

    /**
     * 只查某个用户提交的举报。
     * <p>
     * 由后端从 token 取，不接受前端传入 —— 否则改个参数就能看别人的举报记录。
     * 同一段 SQL 因此能同时服务"我的举报"和"管理后台列表"两个场景：
     * 前者带上这个条件，后者留空。
     */
    @Schema(hidden = true)
    private Long userId;
}
