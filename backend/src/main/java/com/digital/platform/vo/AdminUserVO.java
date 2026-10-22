package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 管理后台 - 用户列表项。
 * <p>
 * 继承 UserVO 拿到基础字段，再补一个 productCount。
 * 管理员判断"要不要禁用某个用户"时，看到他有 30 件在售商品和看到 0 件，
 * 决策是完全不一样的 —— 所以这个数字必须给出来。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台用户信息")
public class AdminUserVO extends UserVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "该用户发布的商品数")
    private Long productCount;
}
