package com.digital.platform.service;

import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.CommentDTO;
import com.digital.platform.entity.Comment;
import com.digital.platform.entity.Product;
import com.digital.platform.mapper.CommentMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 留言服务的单元测试。
 * <p>
 * 第二个测试是**整个测试集里最有价值的一个**：它验证"回复某条回复"时，
 * 数据会被正确归并到同一条一级留言下。
 * <p>
 * 如果不做这个归并，新留言的 parent_id 会指向一条"回复"，
 * 而查询时只取 {@code parent_id IN (一级留言ID)}，结果就是：
 * <b>用户看到"回复成功"，刷新后这条留言永远消失。</b>
 * 这种 bug 用肉眼极难发现，但一条单元测试就能锁住。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("留言服务")
class CommentServiceImplTest {

    private static final long PRODUCT_ID = 10L;
    private static final long PRODUCT_OWNER = 2L;
    private static final long REPLIER = 6L;
    private static final long TOP_LEVEL_ID = 2L;
    private static final long REPLY_ID = 3L;
    private static final long REPLY_AUTHOR = 5L;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    @DisplayName("商品发布者不能给自己的商品发一级留言（防止自问自答刷热度）")
    void create_throws_when_owner_comments_on_own_product() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, PRODUCT_OWNER));

        CommentDTO dto = commentDto(null, "这个多少钱？");

        assertBizCode(ResultCode.CANNOT_COMMENT_SELF,
                () -> commentService.create(PRODUCT_OWNER, "卖家", dto));
    }

    @Test
    @DisplayName("回复某条回复时，归并到同一条一级留言下，但被回复人仍是那条回复的作者")
    void create_merges_reply_to_reply_into_top_level_thread() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, PRODUCT_OWNER));
        // 被点击的是一条"回复"：它的 parentId 指向一级留言 TOP_LEVEL_ID，作者是 REPLY_AUTHOR
        when(commentMapper.selectById(REPLY_ID))
                .thenReturn(comment(REPLY_ID, REPLY_AUTHOR, TOP_LEVEL_ID));

        CommentDTO dto = commentDto(REPLY_ID, "那我明天过来看看");

        commentService.create(REPLIER, "回复者", dto);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        Comment saved = captor.getValue();

        // ① 归并：parent_id 落在一级留言上，而不是那条回复上
        assertThat(saved.getParentId())
                .as("应归并到一级留言，否则这条回复永远查不出来")
                .isEqualTo(TOP_LEVEL_ID);

        // ② 语义：被回复人仍然是那条回复的作者，不是一级留言的作者
        assertThat(saved.getReplyUserId())
                .as("被回复人应该是被点击那条留言的作者")
                .isEqualTo(REPLY_AUTHOR);

        assertThat(saved.getUserId()).isEqualTo(REPLIER);
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
    }

    // ============================================================
    //  辅助方法
    // ============================================================

    private Product product(long id, long ownerId) {
        Product p = new Product();
        p.setId(id);
        p.setUserId(ownerId);
        p.setTitle("测试商品");
        p.setPrice(new BigDecimal("100.00"));
        p.setStatus(ProductStatus.ON_SALE.getCode());
        return p;
    }

    private Comment comment(long id, long userId, long parentId) {
        Comment c = new Comment();
        c.setId(id);
        c.setProductId(PRODUCT_ID);
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setStatus(1);
        c.setContent("已有留言");
        return c;
    }

    private CommentDTO commentDto(Long parentId, String content) {
        CommentDTO dto = new CommentDTO();
        dto.setProductId(PRODUCT_ID);
        dto.setParentId(parentId);
        dto.setContent(content);
        return dto;
    }

    private void assertBizCode(ResultCode expected, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertThat(ex.getCode()).isEqualTo(expected.getCode());
    }
}
