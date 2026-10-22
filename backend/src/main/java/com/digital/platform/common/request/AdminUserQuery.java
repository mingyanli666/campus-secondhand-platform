package com.digital.platform.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 管理后台 - 用户查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询条件")
public class AdminUserQuery extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词，模糊匹配账号、昵称、手机号")
    private String keyword;

    @Schema(description = "角色筛选 USER/ADMIN")
    private String role;

    @Schema(description = "状态筛选 1正常 0禁用")
    private Integer status;
}
