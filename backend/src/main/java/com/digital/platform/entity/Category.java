package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体，对应 sys_category 表。
 */
@Data
@TableName("sys_category")
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String icon;

    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     * 有 @TableLogic 注解的字段，MyBatis-Plus 会自动：
     * - 删除时改写为 UPDATE ... SET deleted = 1
     * - 查询时自动追加 WHERE deleted = 0
     */
    @TableLogic
    private Integer deleted;
}
