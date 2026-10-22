package com.digital.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.NotificationType;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.CommentDTO;
import com.digital.platform.entity.Comment;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.CommentMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.service.CommentService;
import com.digital.platform.service.NotificationService;
import com.digital.platform.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /** 通知正文里截断留言内容，避免超出 sys_notification.content 的 500 字符上限 */
    private static final int NOTIFY_CONTENT_MAX = 200;

    private final CommentMapper commentMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    // ============================================================
    //  发布留言 / 回复
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, String nickname, CommentDTO dto) {
        Product product = requireProduct(dto.getProductId());
        checkProductCommentable(product);

        // 前端不传 parentId 就是一级留言，传了就是回复 —— 一个接口承担两种语义
        long requestedParentId = dto.getParentId() == null ? 0L : dto.getParentId();
        long rootParentId = 0L;
        Long replyUserId = null;
        Long notifyUserId;
        boolean isReply = requestedParentId > 0;

        if (isReply) {
            Comment target = commentMapper.selectById(requestedParentId);
            if (target == null || !Constants.STATUS_ENABLED.equals(target.getStatus())) {
                throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
            }
            // 防止回复到别的商品下面的留言上，那样前端组装两级结构会乱掉
            if (!target.getProductId().equals(product.getId())) {
                throw new BusinessException(ResultCode.COMMENT_PRODUCT_MISMATCH);
            }

            // 被回复的人是这条留言的作者
            replyUserId = target.getUserId();
            notifyUserId = target.getUserId();

            // 关键：把"回复某条回复"归并到同一条一级留言下。
            // 本项目只做两级，如果允许 parent_id 指向一条回复，就会产生第三层，
            // 而查询时只取 parent_id IN (一级留言ID) 的回复 —— 那条留言会永远显示不出来。
            // 归并之后，"被回复人"仍然是那条回复的作者，对话读起来是连贯的。
            rootParentId = target.getParentId() == 0L ? target.getId() : target.getParentId();
        } else {
            // 一级留言（咨询）不能由商品发布者自己发。
            // 否则卖家可以自问自答刷热度，看起来"很受欢迎"。
            if (product.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.CANNOT_COMMENT_SELF);
            }
            notifyUserId = product.getUserId();
        }

        Comment comment = new Comment();
        comment.setProductId(product.getId());
        comment.setUserId(userId);
        comment.setParentId(rootParentId);
        comment.setReplyUserId(replyUserId);
        comment.setContent(dto.getContent());
        comment.setIsRead(Constants.UNREAD);
        comment.setStatus(Constants.STATUS_ENABLED);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        // 通知对方。自己回复自己就不必通知了，否则会收到一堆自己的消息
        if (!notifyUserId.equals(userId)) {
            String title = isReply ? "你的留言收到回复" : "你的商品收到新留言";
            String content = isReply
                    ? nickname + " 回复了你在「" + product.getTitle() + "」下的留言："
                      + StrUtil.maxLength(dto.getContent(), NOTIFY_CONTENT_MAX)
                    : "有人咨询了你发布的「" + product.getTitle() + "」："
                      + StrUtil.maxLength(dto.getContent(), NOTIFY_CONTENT_MAX);
            notificationService.send(notifyUserId, NotificationType.COMMENT, title, content, product.getId());
        }

        return comment.getId();
    }

    // ============================================================
    //  查询：两级结构
    // ============================================================

    @Override
    public List<CommentVO> listByProduct(Long productId, Long currentUserId, boolean isAdmin) {
        Product product = requireProduct(productId);

        // 第 1 次查询：所有一级留言（新的在前）
        List<Comment> topLevel = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getProductId, productId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, Constants.STATUS_ENABLED)
                .orderByDesc(Comment::getCreateTime)
                .orderByDesc(Comment::getId));

        if (topLevel.isEmpty()) {
            return List.of();
        }

        // 第 2 次查询：这些留言下的所有回复（旧的在前，按对话顺序读）
        List<Long> parentIds = topLevel.stream().map(Comment::getId).toList();
        List<Comment> replies = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getProductId, productId)
                .in(Comment::getParentId, parentIds)
                .eq(Comment::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(Comment::getCreateTime)
                .orderByAsc(Comment::getId));

        // 第 3 次查询：批量把用到的用户一次查出来。
        // 绝不能"每条留言查一次用户" —— 20 条留言就是 20 次查询，这就是典型的 N+1 问题。
        Map<Long, User> userMap = loadUsers(topLevel, replies);

        boolean isProductOwner = currentUserId != null && currentUserId.equals(product.getUserId());

        // 按 parentId 把回复分组，再挂到对应的一级留言上
        Map<Long, List<CommentVO>> replyGroup = replies.stream()
                .map(c -> toVO(c, userMap, currentUserId, isProductOwner, isAdmin))
                .collect(Collectors.groupingBy(CommentVO::getParentId));

        return topLevel.stream()
                .map(c -> {
                    CommentVO vo = toVO(c, userMap, currentUserId, isProductOwner, isAdmin);
                    vo.setReplies(replyGroup.getOrDefault(c.getId(), List.of()));
                    return vo;
                })
                .toList();
    }

    // ============================================================
    //  删除
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, boolean isAdmin, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !Constants.STATUS_ENABLED.equals(comment.getStatus())) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }

        Product product = productMapper.selectById(comment.getProductId());
        boolean isAuthor = comment.getUserId().equals(userId);
        // 商品发布者可以删除自己商品下的留言（比如广告、人身攻击）
        boolean isProductOwner = product != null && product.getUserId().equals(userId);

        if (!isAdmin && !isAuthor && !isProductOwner) {
            throw new BusinessException(ResultCode.NOT_COMMENT_OWNER);
        }

        // 软删除：保留记录，状态置为已删除
        commentMapper.update(null, Wrappers.<Comment>lambdaUpdate()
                .set(Comment::getStatus, Constants.STATUS_DISABLED)
                .eq(Comment::getId, commentId));

        // 一级留言被删时，它下面的回复也一起隐藏。
        // 否则列表里会只剩一堆"回复某某"的孤儿，读者完全看不懂在说什么。
        if (0L == comment.getParentId()) {
            commentMapper.update(null, Wrappers.<Comment>lambdaUpdate()
                    .set(Comment::getStatus, Constants.STATUS_DISABLED)
                    .eq(Comment::getParentId, commentId));
        }
    }

    // ============================================================
    //  私有辅助
    // ============================================================

    /**
     * 一次性把所有相关用户查出来，返回 id -> User 的映射。
     * 这就是解决 N+1 问题的标准做法：把"每条记录查一次"变成"一次查一批"。
     */
    private Map<Long, User> loadUsers(List<Comment> topLevel, List<Comment> replies) {
        Set<Long> userIds = new HashSet<>();
        topLevel.forEach(c -> userIds.add(c.getUserId()));
        replies.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyUserId() != null) {
                userIds.add(c.getReplyUserId());
            }
        });

        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private CommentVO toVO(Comment c, Map<Long, User> userMap,
                           Long currentUserId, boolean isProductOwner, boolean isAdmin) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setProductId(c.getProductId());
        vo.setUserId(c.getUserId());
        vo.setParentId(c.getParentId());
        vo.setReplyUserId(c.getReplyUserId());
        vo.setContent(c.getContent());
        vo.setCreateTime(c.getCreateTime());

        User author = userMap.get(c.getUserId());
        if (author != null) {
            vo.setUserNickname(author.getNickname());
            vo.setUserAvatar(author.getAvatar());
        }
        if (c.getReplyUserId() != null) {
            User replied = userMap.get(c.getReplyUserId());
            vo.setReplyUserNickname(replied == null ? null : replied.getNickname());
        }

        // 删除权限由后端算好直接告诉前端，前端不用（也无法）自己判断。
        // 三种人可以删：留言作者本人、商品发布者、平台管理员。
        vo.setCanDelete(currentUserId != null
                && (isAdmin || isProductOwner || currentUserId.equals(c.getUserId())));
        return vo;
    }

    private Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    /** 待审核 / 已下架 / 被驳回的商品不该有留言入口 */
    private void checkProductCommentable(Product product) {
        Integer status = product.getStatus();
        boolean ok = ProductStatus.ON_SALE.getCode().equals(status)
                || ProductStatus.SOLD.getCode().equals(status);
        if (!ok) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "该商品当前不支持留言咨询");
        }
    }
}
