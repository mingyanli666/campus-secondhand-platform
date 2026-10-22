package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知，对应 sys_notification 表。
 * <p>
 * 为什么要有它？因为"审核不通过"这件事如果只改数据库状态，
 * 卖家根本不知道——他只会发现商品不见了。
 * 通知把"平台的决定"主动推给用户，这是需求里"交互逻辑清晰、友好提示"的一部分。
 */
@Data
@TableName("sys_notification")
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long userId;

    /** 1 审核结果 2 订单状态 3 留言回复 4 系统通知 */
    private Integer type;

    private String title;

    private String content;

    /** 关联业务ID（商品ID / 订单ID），点击通知可以跳转过去 */
    private Long bizId;

    /** 0 未读 1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
