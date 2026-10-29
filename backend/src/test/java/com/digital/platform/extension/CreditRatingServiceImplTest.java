package com.digital.platform.extension;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.Order;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.Report;
import com.digital.platform.entity.User;
import com.digital.platform.extension.impl.CreditRatingServiceImpl;
import com.digital.platform.mapper.OrderMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.ReportMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.vo.CreditRatingVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 信誉评级基线实现的测试。
 * <p>
 * 打分公式是「60 起步 + 成交加分 + 发布加分 − 被举报扣分」，
 * 这里逐条验证每个维度真的生效、以及上限封顶有效
 * （否则一个老用户刷到 200 分，或者一次事故把人打到负数）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("拓展-信誉评级（基线实现）")
class CreditRatingServiceImplTest {

    private static final long USER_ID = 101L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ReportMapper reportMapper;

    @InjectMocks
    private CreditRatingServiceImpl service;

    /**
     * 三张表的 TableInfo 都要注册。
     * <p>
     * 注意一个细节：{@code .eq(Product::getUserId, ...)} 是**懒解析**（拼 SQL 时才查列名），
     * 而 {@code .select(Product::getId)} 是**立即解析**。所以只用 eq 的测试不初始化也能过，
     * 用了 select 的就会抛 "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Product.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, Report.class);
    }

    @Test
    @DisplayName("用户不存在时抛 2003")
    void throws_when_user_missing() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.rating(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
        verify(orderMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("全新用户是 60 分「一般」，并说明原因")
    void new_user_gets_base_score() {
        stubCounts(0L, 0L, List.of(), 0L);

        CreditRatingVO vo = service.rating(USER_ID);

        assertThat(vo.getScore()).isEqualTo(60);
        assertThat(vo.getLevel()).isEqualTo("一般");
        assertThat(vo.getDescription()).contains("暂无成交记录");
    }

    @Test
    @DisplayName("成交和发布都加分：10 笔成交 + 5 个商品 → 95 分「优秀」")
    void sold_and_product_counts_add_score() {
        stubCounts(10L, 5L, List.of(1L, 2L), 0L);

        CreditRatingVO vo = service.rating(USER_ID);

        // 60 + 10*3 + 5 = 95
        assertThat(vo.getScore()).isEqualTo(95);
        assertThat(vo.getLevel()).isEqualTo("优秀");
        assertThat(vo.getDescription()).contains("履约记录良好");
    }

    @Test
    @DisplayName("成交数超过上限后不再加分（防止老用户永远满分）")
    void sold_count_is_capped() {
        stubCounts(999L, 999L, List.of(1L), 0L);

        // 60 + 10*3 + 5 = 95，而不是 60 + 999*3
        assertThat(service.rating(USER_ID).getScore()).isEqualTo(95);
    }

    @Test
    @DisplayName("被举报成立会扣分，并在说明里点明原因")
    void handled_reports_deduct_score() {
        stubCounts(10L, 5L, List.of(1L, 2L), 1L);

        CreditRatingVO vo = service.rating(USER_ID);

        // 60 + 30 + 5 - 10 = 85
        assertThat(vo.getScore()).isEqualTo(85);
        assertThat(vo.getLevel()).isEqualTo("良好");
        assertThat(vo.getDescription()).contains("被举报");
    }

    @Test
    @DisplayName("扣分有上限，多次举报也不会把人打到 0 分以下")
    void report_penalty_is_capped() {
        stubCounts(0L, 0L, List.of(1L), 99L);

        CreditRatingVO vo = service.rating(USER_ID);

        // 60 - 5*10 = 10，而不是 60 - 99*10
        assertThat(vo.getScore()).isEqualTo(10);
        assertThat(vo.getLevel()).isEqualTo("较差");
        assertThat(vo.getScore()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("没有发布过商品时不去查举报（避免拼出空的 IN 条件）")
    void skips_report_query_when_no_products() {
        stubCounts(0L, 0L, List.of(), 0L);

        service.rating(USER_ID);

        verify(reportMapper, never()).selectCount(any());
    }

    // ============================================================

    private void stubCounts(long sold, long products, List<Long> productIds, long reports) {
        User u = new User();
        u.setId(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(u);
        when(orderMapper.selectCount(any())).thenReturn(sold);
        when(productMapper.selectCount(any())).thenReturn(products);

        List<Product> owned = productIds.stream().map(id -> {
            Product p = new Product();
            p.setId(id);
            return p;
        }).toList();
        when(productMapper.selectList(any())).thenReturn(owned);

        if (!productIds.isEmpty()) {
            when(reportMapper.selectCount(any())).thenReturn(reports);
        }
    }
}
