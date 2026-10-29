package com.digital.platform.extension;

import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.Product;
import com.digital.platform.extension.impl.PriceEvaluationServiceImpl;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.vo.PriceEvaluationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 价格评估基线实现的测试。
 * <p>
 * 重点在两件事：
 * <ol>
 *   <li><b>样本不足时说"不知道"</b>，而不是拿 1 个样本算出个"均价"误导人</li>
 *   <li><b>边界是 ±15%</b> —— 边界值两侧的结论必须不同</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("拓展-价格评估（基线实现）")
class PriceEvaluationServiceImplTest {

    private static final long PRODUCT_ID = 101L;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private PriceEvaluationServiceImpl service;

    @Test
    @DisplayName("商品不存在时抛 3001")
    void throws_when_product_missing() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.evaluate(PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("同类商品少于 3 件时给「数据不足」，不硬算均价")
    void reports_insufficient_sample() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product("2799"));
        when(productMapper.selectList(any())).thenReturn(List.of(product("2500"), product("2600")));

        PriceEvaluationVO vo = service.evaluate(PRODUCT_ID);

        assertThat(vo.getLevel()).isEqualTo("数据不足");
        assertThat(vo.getSampleCount()).isEqualTo(2);
        assertThat(vo.getAveragePrice()).isNull();
        assertThat(vo.getAdvice()).contains("样本太少");
    }

    @Test
    @DisplayName("明显低于均价 → 偏低")
    void detects_low_price() {
        // 当前 800，样本均价约 1000 → 低 20%
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product("800"));
        when(productMapper.selectList(any()))
                .thenReturn(List.of(product("1000"), product("1000"), product("1000")));

        PriceEvaluationVO vo = service.evaluate(PRODUCT_ID);

        assertThat(vo.getLevel()).isEqualTo("偏低");
        assertThat(vo.getAveragePrice()).isEqualByComparingTo("1000.00");
        assertThat(vo.getMinPrice()).isEqualByComparingTo("1000.00");
        assertThat(vo.getMaxPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("明显高于均价 → 偏高")
    void detects_high_price() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product("1200"));
        when(productMapper.selectList(any()))
                .thenReturn(List.of(product("1000"), product("1000"), product("1000")));

        assertThat(service.evaluate(PRODUCT_ID).getLevel()).isEqualTo("偏高");
    }

    @Test
    @DisplayName("偏离在 ±15% 以内 → 合理")
    void detects_reasonable_price() {
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product("1050"));
        when(productMapper.selectList(any()))
                .thenReturn(List.of(product("1000"), product("1000"), product("1000")));

        PriceEvaluationVO vo = service.evaluate(PRODUCT_ID);

        assertThat(vo.getLevel()).isEqualTo("合理");
        assertThat(vo.getSampleCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("恰好卡在 +15% 边界上算合理，超过才算偏高（边界值）")
    void boundary_at_15_percent_is_reasonable() {
        // 当前 1150，均价 1000 → 正好 +15%
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product("1150"));
        when(productMapper.selectList(any()))
                .thenReturn(List.of(product("1000"), product("1000"), product("1000")));
        assertThat(service.evaluate(PRODUCT_ID).getLevel()).isEqualTo("合理");
    }

    private Product product(String price) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setCategoryId(1L);
        p.setUserId(9L);
        p.setConditionLevel("99新");
        p.setPrice(new BigDecimal(price));
        p.setStatus(ProductStatus.ON_SALE.getCode());
        return p;
    }
}
