package com.digital.platform.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.digital.platform.cache.ProductCache;
import com.digital.platform.cache.ViewCountCache;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.ProductSaveDTO;
import com.digital.platform.entity.Category;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.CategoryMapper;
import com.digital.platform.mapper.ProductImageMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.service.impl.ProductServiceImpl;
import com.digital.platform.vo.ProductDetailVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品服务的单元测试。
 * <p>
 * 商品是整条业务链的起点，这里的规则一旦漏测，后果都不是「页面不好看」：
 * <ul>
 *   <li>不校验归属 → 任何人能改、能删别人的商品</li>
 *   <li>编辑不重置审核状态 → 先发合格内容过审，再改成违规内容</li>
 *   <li>写操作不删缓存 → 用户改了价格，访客看到的还是旧价</li>
 *   <li>未上架商品不做权限判断 → 待审核/被驳回的内容对外泄漏</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品服务 - 归属校验、状态流转、缓存一致性")
class ProductServiceImplTest {

    private static final long OWNER = 7L;
    private static final long OTHER = 8L;
    private static final long ADMIN = 1L;
    private static final long PRODUCT_ID = 100L;
    private static final long CATEGORY_ID = 3L;

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageMapper productImageMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FavoriteService favoriteService;
    @Mock
    private ProductCache productCache;
    @Mock
    private ViewCountCache viewCountCache;

    @InjectMocks
    private ProductServiceImpl productService;

    /**
     * LambdaUpdateWrapper 在 `.set(Product::getStatus, ...)` 时需要把方法引用
     * 解析成数据库列名，这依赖 MyBatis-Plus 的 TableInfo 已经注册。
     * 纯单元测试没有 Spring 上下文，所以要手动初始化一次。
     * <p>
     * 不做这一步的话，resubmit / update 里那两个「显式 set」的调用会直接抛异常 ——
     * 而这两个方法恰恰是最需要被测到的（它们就是为了修一个真实 bug 才写成这样的）。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
    }

    // ============================================================
    //  发布
    // ============================================================

    @Nested
    @DisplayName("发布商品")
    class Publish {

        @Test
        @DisplayName("分类不存在时拒绝发布")
        void throws_when_category_missing() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(null);

            assertBusinessCode(
                    () -> productService.publish(OWNER, dto()),
                    ResultCode.PARAM_ERROR);
        }

        @Test
        @DisplayName("分类被停用时拒绝发布（停用分类不该还能挂商品）")
        void throws_when_category_disabled() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category(0));

            assertBusinessCode(
                    () -> productService.publish(OWNER, dto()),
                    ResultCode.PARAM_ERROR);
        }

        @Test
        @DisplayName("发布后进入待审核，第一张图作为封面")
        void publish_sets_pending_status_and_uses_first_image_as_cover() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category(1));
            when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
                inv.getArgument(0, Product.class).setId(PRODUCT_ID);
                return 1;
            });

            ProductSaveDTO dto = dto();
            dto.setImages(List.of("/uploads/a.png", "/uploads/b.png"));
            productService.publish(OWNER, dto);

            verify(productMapper).insert(org.mockito.ArgumentMatchers.argThat(p ->
                    ProductStatus.PENDING.getCode().equals(p.getStatus())
                            && Long.valueOf(OWNER).equals(p.getUserId())
                            && "/uploads/a.png".equals(p.getCoverImage())
                            && p.getViewCount() == 0
                            && p.getFavoriteCount() == 0));
            // 两张图都要落库
            verify(productImageMapper, org.mockito.Mockito.times(2)).insert(any());
            // 新商品要清掉可能存在的"空值哨兵"缓存
            verify(productCache).evict(PRODUCT_ID);
        }

        @Test
        @DisplayName("没有图片时封面为空，不报错")
        void publish_allows_empty_images() {
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category(1));
            when(productMapper.insert(any(Product.class))).thenAnswer(inv -> {
                inv.getArgument(0, Product.class).setId(PRODUCT_ID);
                return 1;
            });

            ProductSaveDTO dto = dto();
            dto.setImages(List.of());
            productService.publish(OWNER, dto);

            verify(productMapper).insert(org.mockito.ArgumentMatchers.argThat(p ->
                    p.getCoverImage() == null));
            verify(productImageMapper, never()).insert(any());
        }
    }

    // ============================================================
    //  编辑
    // ============================================================

    @Nested
    @DisplayName("编辑商品")
    class Update {

        @Test
        @DisplayName("不能编辑别人的商品")
        void throws_when_not_owner() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.ON_SALE));

            assertBusinessCode(
                    () -> productService.update(OTHER, PRODUCT_ID, dto()),
                    ResultCode.NOT_PRODUCT_OWNER);
        }

        @Test
        @DisplayName("商品不存在时按「不存在」处理，不泄漏它存在过")
        void throws_when_product_missing() {
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

            assertBusinessCode(
                    () -> productService.update(OWNER, PRODUCT_ID, dto()),
                    ResultCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("已售出的商品不能编辑（否则和买家下单时看到的信息对不上）")
        void throws_when_sold() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.SOLD));
            // 注意顺序：update 里是先校验分类、再判断商品状态，
            // 所以这里必须把分类 stub 掉，否则先撞上的是「分类不存在」
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category(1));

            assertBusinessCode(
                    () -> productService.update(OWNER, PRODUCT_ID, dto()),
                    ResultCode.PRODUCT_STATUS_ILLEGAL);
        }

        @Test
        @DisplayName("编辑后必须重新回到待审核，防止过审后再改成违规内容")
        void update_resets_status_to_pending() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.ON_SALE));
            when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category(1));

            productService.update(OWNER, PRODUCT_ID, dto());

            verify(productMapper).updateById(org.mockito.ArgumentMatchers.argThat(p ->
                    ProductStatus.PENDING.getCode().equals(p.getStatus())));
            verify(productCache).evict(PRODUCT_ID);
        }
    }

    // ============================================================
    //  删除 / 下架 / 重新提交
    // ============================================================

    @Nested
    @DisplayName("删除与状态流转")
    class Lifecycle {

        @Test
        @DisplayName("已售出的商品不能删除")
        void delete_throws_when_sold() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.SOLD));

            assertBusinessCode(
                    () -> productService.delete(OWNER, PRODUCT_ID),
                    ResultCode.PRODUCT_STATUS_ILLEGAL);
        }

        @Test
        @DisplayName("删除商品要同时清掉图片和缓存")
        void delete_removes_images_and_cache() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.ON_SALE));

            productService.delete(OWNER, PRODUCT_ID);

            verify(productMapper).deleteById(PRODUCT_ID);
            verify(productImageMapper).delete(any());
            verify(productCache).evict(PRODUCT_ID);
        }

        @Test
        @DisplayName("只有在售商品能下架")
        void offShelf_rejects_non_on_sale() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.PENDING));

            assertBusinessCode(
                    () -> productService.offShelf(OWNER, PRODUCT_ID),
                    ResultCode.PRODUCT_STATUS_ILLEGAL);
        }

        @Test
        @DisplayName("在售商品下架成功并清缓存")
        void offShelf_sets_status_and_evicts() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.ON_SALE));

            productService.offShelf(OWNER, PRODUCT_ID);

            verify(productMapper).updateById(org.mockito.ArgumentMatchers.argThat(p ->
                    ProductStatus.OFF_SHELF.getCode().equals(p.getStatus())));
            verify(productCache).evict(PRODUCT_ID);
        }

        @Test
        @DisplayName("在售商品不能重新提交审核（它本来就已经过审了）")
        void resubmit_rejects_on_sale() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.ON_SALE));

            assertBusinessCode(
                    () -> productService.resubmit(OWNER, PRODUCT_ID),
                    ResultCode.PRODUCT_STATUS_ILLEGAL);
        }

        @Test
        @DisplayName("被驳回的商品可以重新提交，并清空驳回原因")
        void resubmit_from_rejected_clears_reason() {
            when(productMapper.selectById(PRODUCT_ID))
                    .thenReturn(product(OWNER, ProductStatus.REJECTED));

            productService.resubmit(OWNER, PRODUCT_ID);

            // 用 UpdateWrapper 显式 set，而不是 updateById —— 后者会跳过 null 字段
            verify(productMapper).update(eq(null), any());
            verify(productCache).evict(PRODUCT_ID);
        }
    }

    // ============================================================
    //  详情（缓存 + 权限 + 浏览量）
    // ============================================================

    @Nested
    @DisplayName("商品详情")
    class Detail {

        @Test
        @DisplayName("已被标记为不存在的 ID 直接返回，不再查库（缓存穿透防护）")
        void detail_short_circuits_for_known_missing() {
            when(productCache.isKnownMissing(PRODUCT_ID)).thenReturn(true);

            assertBusinessCode(
                    () -> productService.detail(PRODUCT_ID, null),
                    ResultCode.PRODUCT_NOT_FOUND);

            verify(productMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("商品确实不存在时写入空值哨兵，避免被反复打库")
        void detail_puts_empty_sentinel_when_missing() {
            when(productCache.isKnownMissing(PRODUCT_ID)).thenReturn(false);
            when(productCache.getDetail(PRODUCT_ID)).thenReturn(null);
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

            assertBusinessCode(
                    () -> productService.detail(PRODUCT_ID, null),
                    ResultCode.PRODUCT_NOT_FOUND);

            verify(productCache).putEmpty(PRODUCT_ID);
        }

        @Test
        @DisplayName("未上架商品对外表现为「不存在」，不泄漏存在性")
        void detail_hides_off_shelf_product_from_stranger() {
            when(productCache.getDetail(PRODUCT_ID))
                    .thenReturn(detailVO(ProductStatus.PENDING, OWNER));

            assertBusinessCode(
                    () -> productService.detail(PRODUCT_ID, OTHER),
                    ResultCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("管理员可以查看未上架商品（审核需要）")
        void detail_allows_admin_to_see_off_shelf() {
            when(productCache.getDetail(PRODUCT_ID))
                    .thenReturn(detailVO(ProductStatus.PENDING, OWNER));
            when(userMapper.selectById(ADMIN)).thenReturn(user(ADMIN, "ADMIN"));
            when(viewCountCache.increment(PRODUCT_ID)).thenReturn(0L);

            ProductDetailVO vo = productService.detail(PRODUCT_ID, ADMIN);

            assertThat(vo.getStatus()).isEqualTo(ProductStatus.PENDING.getCode());
        }

        @Test
        @DisplayName("本人查看自己的商品不计浏览量")
        void detail_does_not_count_owner_view() {
            when(productCache.getDetail(PRODUCT_ID))
                    .thenReturn(detailVO(ProductStatus.ON_SALE, OWNER));

            ProductDetailVO vo = productService.detail(PRODUCT_ID, OWNER);

            assertThat(vo.getOwner()).isTrue();
            verify(viewCountCache, never()).increment(any());
        }

        @Test
        @DisplayName("浏览量 = 缓存基线 + Redis 增量")
        void detail_adds_redis_delta_to_view_count() {
            ProductDetailVO cached = detailVO(ProductStatus.ON_SALE, OWNER);
            cached.setViewCount(100);
            when(productCache.getDetail(PRODUCT_ID)).thenReturn(cached);
            when(favoriteService.isFavorite(OTHER, PRODUCT_ID)).thenReturn(true);
            when(viewCountCache.increment(PRODUCT_ID)).thenReturn(5L);

            ProductDetailVO vo = productService.detail(PRODUCT_ID, OTHER);

            assertThat(vo.getViewCount()).isEqualTo(105);
            assertThat(vo.getFavorite()).isTrue();
            assertThat(vo.getOwner()).isFalse();
        }

        @Test
        @DisplayName("Redis 不可用时降级为直接改库，计数不丢")
        void detail_falls_back_to_db_when_redis_down() {
            ProductDetailVO cached = detailVO(ProductStatus.ON_SALE, OWNER);
            cached.setViewCount(100);
            when(productCache.getDetail(PRODUCT_ID)).thenReturn(cached);
            when(viewCountCache.increment(PRODUCT_ID)).thenReturn(-1L);

            ProductDetailVO vo = productService.detail(PRODUCT_ID, OTHER);

            assertThat(vo.getViewCount()).isEqualTo(101);
            verify(productMapper).addViewCount(PRODUCT_ID, 1L);
        }
    }

    // ============================================================
    //  测试数据构造
    // ============================================================

    private ProductSaveDTO dto() {
        ProductSaveDTO dto = new ProductSaveDTO();
        dto.setCategoryId(CATEGORY_ID);
        dto.setTitle("iPhone 13 128G");
        dto.setDescription("国行，无拆修");
        dto.setPrice(new BigDecimal("2999.00"));
        dto.setConditionLevel("99新");
        return dto;
    }

    private Product product(long userId, ProductStatus status) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setUserId(userId);
        p.setStatus(status.getCode());
        p.setTitle("iPhone 13 128G");
        p.setCategoryId(CATEGORY_ID);
        p.setViewCount(0);
        p.setFavoriteCount(0);
        return p;
    }

    private Category category(int status) {
        Category c = new Category();
        c.setId(CATEGORY_ID);
        c.setName("手机");
        c.setStatus(status);
        return c;
    }

    private User user(long id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setStatus(Constants.STATUS_ENABLED);
        return u;
    }

    private ProductDetailVO detailVO(ProductStatus status, long sellerId) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(PRODUCT_ID);
        vo.setStatus(status.getCode());
        vo.setSellerId(sellerId);
        vo.setTitle("iPhone 13 128G");
        vo.setViewCount(0);
        return vo;
    }

    private void assertBusinessCode(org.junit.jupiter.api.function.Executable action,
                                    ResultCode expected) {
        assertThatThrownBy(action::execute)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(expected.getCode());
    }
}
