package com.digital.platform.service;

import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.Product;
import com.digital.platform.mapper.FavoriteMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 收藏服务的单元测试。
 * <p>
 * 重点在两个"数据一致性"相关的分支：
 * <ul>
 *   <li>不能收藏自己的商品 —— 否则卖家可以自己刷收藏数，制造"很抢手"的假象</li>
 *   <li>并发重复收藏 —— 靠唯一索引兜底，要把数据库异常翻译成用户能看懂的提示</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("收藏服务")
class FavoriteServiceImplTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;
    private static final long PRODUCT_ID = 10L;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @Test
    @DisplayName("不能收藏自己发布的商品")
    void add_throws_when_product_belongs_to_self() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, ME));

        assertBizCode(ResultCode.FAVORITE_SELF,
                () -> favoriteService.add(ME, PRODUCT_ID));
    }

    @Test
    @DisplayName("重复收藏时把数据库唯一键冲突翻译成友好提示")
    void add_translates_duplicate_key_to_already_favorite() {
        when(productMapper.selectById(PRODUCT_ID))
                .thenReturn(product(PRODUCT_ID, OTHER));
        // 模拟并发：两个请求同时通过"是否已收藏"的检查，第二条插入撞上唯一索引
        when(favoriteMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("uk_user_product"));

        // 用户不应该看到 "Duplicate entry ... for key 'uk_user_product'"
        assertBizCode(ResultCode.ALREADY_FAVORITE,
                () -> favoriteService.add(ME, PRODUCT_ID));
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

    private void assertBizCode(ResultCode expected, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertThat(ex.getCode()).isEqualTo(expected.getCode());
    }
}
