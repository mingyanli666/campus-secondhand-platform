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
 * 商品举报，对应 biz_report 表。
 */
@Data
@TableName("biz_report")
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被举报的商品 */
    private Long productId;

    /** 举报人 */
    private Long userId;

    /** 1 虚假信息 2 违禁物品 3 价格欺诈 4 其他 */
    private Integer type;

    private String reason;

    /** 0 待处理 1 已处理 2 已驳回 */
    private Integer status;

    /** 处理结论，会通知给举报人 */
    private String handleResult;

    private Long handlerId;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
