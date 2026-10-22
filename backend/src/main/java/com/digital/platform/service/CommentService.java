package com.digital.platform.service;

import com.digital.platform.dto.CommentDTO;
import com.digital.platform.vo.CommentVO;

import java.util.List;

public interface CommentService {

    /** 发布留言或回复，返回新留言ID */
    Long create(Long userId, String nickname, CommentDTO dto);

    /**
     * 查询某个商品下的留言（两级结构）。
     *
     * @param currentUserId 当前登录用户，可为 null（未登录也能看留言）
     * @param isAdmin       当前用户是否管理员，用于计算每条留言的 canDelete
     */
    List<CommentVO> listByProduct(Long productId, Long currentUserId, boolean isAdmin);

    /**
     * 删除留言。
     *
     * @param isAdmin 管理员可以删除任何留言
     */
    void delete(Long userId, boolean isAdmin, Long commentId);
}
