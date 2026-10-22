package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品收藏，对应 biz_favorite 表。
 * <p>
 * 注意这张表**没有 deleted 字段**，取消收藏就是物理删除。
 * 这是第一阶段特意做的设计：如果收藏也用逻辑删除，
 * (user_id, product_id) 这个唯一索引会和它打架 ——
 * 取消收藏后置 deleted=1，再收藏一次插入新行就会撞唯一键。
 */
@Data
@TableName("biz_favorite")
public class Favorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    private LocalDateTime createTime;
}
