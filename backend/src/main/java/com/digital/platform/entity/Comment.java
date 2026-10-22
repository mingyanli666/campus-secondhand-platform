package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品留言咨询，对应 biz_comment 表。
 * <p>
 * 用 parent_id 实现两级结构：
 * <pre>
 *   parent_id = 0  → 对商品的一级留言（咨询）
 *   parent_id = X  → 回复第 X 条留言
 * </pre>
 * 只做两级而不是无限嵌套，是因为二手交易的留言场景就是"买家问、卖家答"，
 * 无限嵌套的树在界面上反而难用。**够用就好。**
 * <p>
 * 同样没有 deleted 字段：用 status 做软删除，
 * 这样"留言被删了"这件事本身也能被追溯（谁删的、什么时候还能查）。
 */
@Data
@TableName("biz_comment")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 留言人 */
    private Long userId;

    /** 父留言ID，0 表示一级留言 */
    private Long parentId;

    /** 被回复人ID（只有回复才有） */
    private Long replyUserId;

    private String content;

    /** 商品发布者是否已读 */
    private Integer isRead;

    /** 1 正常 0 已删除 */
    private Integer status;

    private LocalDateTime createTime;
}
