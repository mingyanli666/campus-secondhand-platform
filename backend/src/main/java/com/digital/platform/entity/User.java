package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体，对应 sys_user 表。
 * <p>
 * 注意：这里故意没有 deleted 字段。用户不做逻辑删除，用 status 控制启用/禁用，
 * 这样就避免了"逻辑删除 + username 唯一索引"互相打架的经典坑。
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密文，任何情况下都不允许返回给前端 */
    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer gender;

    private String campus;

    private String intro;

    /** USER / ADMIN */
    private String role;

    /** 1 正常 0 禁用 */
    private Integer status;

    private Integer creditScore;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
