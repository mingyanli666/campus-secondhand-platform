package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品图片实体，对应 biz_product_image 表。
 * 一个商品对应多张图，所以抽成独立的表（一对多），而不是塞在商品表里。
 */
@Data
@TableName("biz_product_image")
public class ProductImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String url;

    private Integer sort;

    private LocalDateTime createTime;
}
