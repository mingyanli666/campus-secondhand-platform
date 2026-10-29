package com.digital.platform.extension;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.entity.Product;
import com.digital.platform.extension.impl.RecommendServiceImpl;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.vo.ProductListVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能推荐基线实现的测试。
 * <p>
 * 推荐位是「锦上添花」的功能，所以这里的重点不是排序准不准，
 * 而是**它不能把主流程搞挂**：
 * 商品不存在时返回空列表而不是抛异常、limit 传超大值时不能把全表拉出来。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("拓展-智能推荐（基线实现）")
class RecommendServiceImplTest {

    private static final long PRODUCT_ID = 101L;
    private static final long CATEGORY_ID = 1L;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private RecommendServiceImpl recommendService;

    @Test
    @DisplayName("商品不存在时返回空列表，不抛异常（推荐位不该拖垮详情页）")
    void similar_returns_empty_when_product_missing() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

        assertThat(recommendService.similarProducts(PRODUCT_ID, 4)).isEmpty();
    }

    @Test
    @DisplayName("相似商品映射成列表 VO，卖家 ID 取自 product.userId")
    void similar_maps_entity_to_list_vo() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product(PRODUCT_ID, CATEGORY_ID));
        when(productMapper.selectPage(any(), any())).thenReturn(
                new Page<Product>(1, 4).setRecords(List.of(product(102L, CATEGORY_ID))));

        List<ProductListVO> list = recommendService.similarProducts(PRODUCT_ID, 4);

        assertThat(list).hasSize(1);
        ProductListVO vo = list.get(0);
        assertThat(vo.getId()).isEqualTo(102L);
        assertThat(vo.getSellerId()).isEqualTo(9L);
        assertThat(vo.getTitle()).isNotBlank();
    }

    @Test
    @DisplayName("limit 超过上限时被限制在 20，防止一次拉全表")
    void hot_clamps_limit() {
        when(productMapper.selectPage(any(), any()))
                .thenReturn(new Page<Product>(1, 20).setRecords(List.of()));

        recommendService.hotProducts(99999);

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(productMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("limit 为 0 或负数时归到 1")
    void hot_normalizes_non_positive_limit() {
        when(productMapper.selectPage(any(), any()))
                .thenReturn(new Page<Product>(1, 1).setRecords(List.of()));

        recommendService.hotProducts(-5);

        ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(productMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getSize()).isEqualTo(1);
    }

    private Product product(long id, long categoryId) {
        Product p = new Product();
        p.setId(id);
        p.setUserId(9L);
        p.setCategoryId(categoryId);
        p.setTitle("iPhone 13 128G");
        p.setPrice(new BigDecimal("2799.00"));
        p.setStatus(ProductStatus.ON_SALE.getCode());
        p.setViewCount(100);
        p.setFavoriteCount(2);
        return p;
    }
}
