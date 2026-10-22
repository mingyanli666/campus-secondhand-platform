package com.digital.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品审核记录，对应 biz_audit_record 表。
 * <p>
 * 这张表是**追加型**的：只插入，从不修改、从不删除。
 * 每一次审核动作都留下一行，"什么时候、谁、对哪个商品、做了什么决定、为什么"
 * 全部可查。这就是需求里"所有发布信息可全程追溯"的落地方式。
 * <p>
 * 另外存了 product_title 和 admin_name 快照：商品改标题、管理员改昵称之后，
 * 历史记录里显示的仍然是"当时"的信息，不会被后续修改污染。
 */
@Data
@TableName("biz_audit_record")
public class AuditRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 商品标题快照 */
    private String productTitle;

    private Long adminId;

    /** 管理员账号快照 */
    private String adminName;

    /** 1 通过 2 驳回 3 强制下架 */
    private Integer action;

    private String reason;

    private LocalDateTime createTime;
}
