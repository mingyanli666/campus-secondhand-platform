package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 卖家的公开资料。
 * <p>
 * 和 UserVO 的区别是什么？**UserVO 是"我看我自己"，这个是"我看别人"。**
 * <p>
 * 所以这里刻意**不包含**任何隐私字段：
 * <ul>
 *   <li>username —— 登录账号，暴露了就能被拿去撞库/枚举</li>
 *   <li>phone / email —— 联系方式应该由卖家在交易时主动给出，而不是挂在主页上任人采集</li>
 *   <li>status / lastLoginTime —— 账号状态和登录时间属于平台运营数据</li>
 * </ul>
 * 只暴露"别的用户需要知道的"：他是谁、信誉如何、在卖什么。
 */
@Data
@Schema(description = "卖家公开资料")
public class UserPublicVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "所在学校/区域")
    private String campus;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "信誉分")
    private Integer creditScore;

    @Schema(description = "加入时间")
    private LocalDateTime createTime;

    @Schema(description = "在售商品数")
    private Long onSaleCount;

    @Schema(description = "已售出商品数")
    private Long soldCount;
}
