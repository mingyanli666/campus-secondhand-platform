package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息视图对象。
 * <p>
 * 为什么不直接返回 User 实体？因为实体里有 password 字段，
 * 一旦手滑序列化出去就是严重的安全事故。用 VO 从结构上杜绝这种可能。
 * 这是"数据安全"要求里最基础也最重要的一条。
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "所在学校/区域")
    private String campus;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "角色 USER/ADMIN")
    private String role;

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;

    @Schema(description = "信誉分")
    private Integer creditScore;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
