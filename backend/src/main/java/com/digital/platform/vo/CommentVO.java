package com.digital.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 留言视图对象。
 * <p>
 * 一级留言的 replies 里装着它的回复；回复自己的 replies 为 null。
 * 只做两级，前端渲染起来简单直接。
 */
@Data
@Schema(description = "留言")
public class CommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    @Schema(description = "留言人ID")
    private Long userId;

    @Schema(description = "留言人昵称")
    private String userNickname;

    @Schema(description = "留言人头像")
    private String userAvatar;

    private Long parentId;

    @Schema(description = "被回复人ID")
    private Long replyUserId;

    @Schema(description = "被回复人昵称")
    private String replyUserNickname;

    private String content;

    private LocalDateTime createTime;

    @Schema(description = "当前登录用户是否有权删除这条留言")
    private Boolean canDelete;

    @Schema(description = "回复列表")
    private List<CommentVO> replies;
}
