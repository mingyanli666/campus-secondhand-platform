package com.digital.platform.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.cache.ProductCache;
import com.digital.platform.cache.ViewCountCache;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.enums.RoleType;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.ProductSaveDTO;
import com.digital.platform.entity.Category;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.ProductImage;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.CategoryMapper;
import com.digital.platform.mapper.ProductImageMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.service.FavoriteService;
import com.digital.platform.service.ProductService;
import com.digital.platform.vo.ProductDetailVO;
import com.digital.platform.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final FavoriteService favoriteService;
    private final ProductCache productCache;
    private final ViewCountCache viewCountCache;

    // ============================================================
    //  发布 / 编辑
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, ProductSaveDTO dto) {
        checkCategory(dto.getCategoryId());

        Product product = new Product();
        copyFields(product, dto);
        product.setUserId(userId);
        // 发布后进入待审核，只有管理员审核通过才会出现在公开列表里
        product.setStatus(ProductStatus.PENDING.getCode());
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setCoverImage(CollUtil.isEmpty(dto.getImages()) ? null : dto.getImages().get(0));
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        productMapper.insert(product);
        saveImages(product.getId(), dto.getImages());
        // 极端情况下这个 id 之前可能被缓存成"不存在"（空值哨兵），顺手清掉
        productCache.evict(product.getId());

        log.info("用户 {} 发布商品 {}，状态：待审核", userId, product.getId());
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long productId, ProductSaveDTO dto) {
        Product exist = requireOwner(userId, productId);
        checkCategory(dto.getCategoryId());

        // 已售出的商品不能改，否则买家看到的信息和下单时不一致
        if (ProductStatus.SOLD.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "已售出的商品不能编辑");
        }

        copyFields(exist, dto);
        exist.setCoverImage(CollUtil.isEmpty(dto.getImages()) ? null : dto.getImages().get(0));
        // 内容变了就要重新审核，防止"先发合格内容过审，再改成违规内容"
        exist.setStatus(ProductStatus.PENDING.getCode());
        exist.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(exist);

        // 单独清空平台处置原因。
        // 不能写成 exist.setRejectReason(null) + updateById —— updateById 会跳过 null 字段，
        // 那句 set 其实不生效，商品会带着上一轮的驳回原因进入新一轮审核。
        clearRejectReason(productId);

        // 图片是"整体替换"语义：先清掉旧的再插新的，比逐个比对简单可靠
        productImageMapper.delete(
                Wrappers.<ProductImage>lambdaQuery().eq(ProductImage::getProductId, productId));
        saveImages(productId, dto.getImages());

        // 内容变了，缓存立刻作废 —— 否则下一个访客看到的还是旧价格
        productCache.evict(productId);
    }

    // ============================================================
    //  删除 / 上下架
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long productId) {
        Product exist = requireOwner(userId, productId);
        if (ProductStatus.SOLD.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "已售出的商品不能删除");
        }
        // 商品走逻辑删除（可追溯），图片记录没有必要留，物理删除
        productMapper.deleteById(productId);
        productImageMapper.delete(
                Wrappers.<ProductImage>lambdaQuery().eq(ProductImage::getProductId, productId));
        productCache.evict(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long userId, Long productId) {
        Product exist = requireOwner(userId, productId);
        if (!ProductStatus.ON_SALE.getCode().equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "只有在售商品才能下架");
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(ProductStatus.OFF_SHELF.getCode());
        update.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(update);

        productCache.evict(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long userId, Long productId) {
        Product exist = requireOwner(userId, productId);
        Integer status = exist.getStatus();
        boolean canResubmit = ProductStatus.OFF_SHELF.getCode().equals(status)
                || ProductStatus.REJECTED.getCode().equals(status);
        if (!canResubmit) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "当前状态不能重新提交审核");
        }
        // 用 UpdateWrapper 显式指定要写的字段，这样 rejectReason 才能真正被置为 null
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getStatus, ProductStatus.PENDING.getCode())
                .set(Product::getRejectReason, null)
                .set(Product::getUpdateTime, LocalDateTime.now())
                .eq(Product::getId, productId));

        productCache.evict(productId);
    }

    // ============================================================
    //  查询
    // ============================================================

    @Override
    public PageResult<ProductListVO> page(ProductQuery query) {
        // 关键词前后空格会导致 LIKE '% 手机 %' 匹配不到，先清理
        if (StrUtil.isNotBlank(query.getKeyword())) {
            query.setKeyword(query.getKeyword().trim());
        }
        Page<ProductListVO> page = query.toPage();
        IPage<ProductListVO> result = productMapper.selectProductPage(page, query);
        return PageResult.of(result);
    }

    @Override
    public ProductDetailVO detail(Long productId, Long currentUserId) {
        // ① 缓存穿透防护：已经确认过不存在的 ID，直接返回，不再打数据库
        if (productCache.isKnownMissing(productId)) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // ② 读缓存。命中就省掉了 商品 + 图片 + 分类 + 卖家 这 4 次查询
        ProductDetailVO vo = productCache.getDetail(productId);
        if (vo == null) {
            vo = buildDetail(productId);
            if (vo == null) {
                // 商品确实不存在：写入空值哨兵（TTL 2 分钟），
                // 防止有人用同一个不存在的 ID 反复请求把数据库打穿
                productCache.putEmpty(productId);
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
            productCache.putDetail(productId, vo);
        }

        // ③ 权限校验。直接用缓存里的 status / sellerId，不需要额外查库
        boolean owner = currentUserId != null && currentUserId.equals(vo.getSellerId());
        if (!ProductStatus.ON_SALE.getCode().equals(vo.getStatus()) && !owner) {
            if (currentUserId == null || !isAdmin(currentUserId)) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
        }

        // ④ 用户相关字段绝不能进缓存，每次按当前用户实时计算
        vo.setOwner(owner);
        vo.setFavorite(currentUserId != null && favoriteService.isFavorite(currentUserId, productId));

        // ⑤ 浏览量：Redis 自增 + 定时批量回写，避免每次浏览都对同一行做 UPDATE
        if (!owner) {
            long delta = viewCountCache.increment(productId);
            if (delta < 0) {
                // Redis 不可用：降级为直接改库，保证计数不丢
                productMapper.addViewCount(productId, 1L);
                vo.setViewCount(vo.getViewCount() + 1);
            } else {
                // 展示值 = 缓存基线 + 自缓存建立以来的增量
                vo.setViewCount(vo.getViewCount() + (int) delta);
            }
        }

        return vo;
    }

    /**
     * 从数据库组装商品详情（4 次查询：商品、图片、分类、卖家）。
     * <p>
     * 只填<b>与当前用户无关</b>的字段 —— owner / favorite 这类用户相关字段
     * 绝不能进缓存，否则张三打开同一个商品会看到李四的收藏状态。
     *
     * @return 商品不存在时返回 null
     */
    private ProductDetailVO buildDetail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return null;
        }

        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setTitle(product.getTitle());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setCoverImage(product.getCoverImage());
        vo.setBrand(product.getBrand());
        vo.setModel(product.getModel());
        vo.setConditionLevel(product.getConditionLevel());
        vo.setSpec(product.getSpec());
        vo.setWarranty(product.getWarranty());
        vo.setPurchaseDate(product.getPurchaseDate());
        vo.setTradeLocation(product.getTradeLocation());
        vo.setContact(product.getContact());
        vo.setStatus(product.getStatus());
        vo.setRejectReason(product.getRejectReason());
        vo.setViewCount(product.getViewCount());
        vo.setFavoriteCount(product.getFavoriteCount());
        vo.setCreateTime(product.getCreateTime());
        vo.setCategoryId(product.getCategoryId());

        // 图片列表
        List<ProductImage> images = productImageMapper.selectList(
                Wrappers.<ProductImage>lambdaQuery()
                        .eq(ProductImage::getProductId, productId)
                        .orderByAsc(ProductImage::getSort));
        vo.setImages(images.stream().map(ProductImage::getUrl).toList());

        // 分类名
        Category category = categoryMapper.selectById(product.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());

        // 卖家信息
        User seller = userMapper.selectById(product.getUserId());
        if (seller != null) {
            vo.setSellerId(seller.getId());
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
            vo.setSellerCreditScore(seller.getCreditScore());
            vo.setSellerCampus(seller.getCampus());
        }

        return vo;
    }

    // ============================================================
    //  私有辅助方法
    // ============================================================

    /** 把 DTO 的业务字段复制到实体，避免 publish / update 两处重复写 */
    private void copyFields(Product product, ProductSaveDTO dto) {
        product.setCategoryId(dto.getCategoryId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setBrand(dto.getBrand());
        product.setModel(dto.getModel());
        product.setConditionLevel(dto.getConditionLevel());
        product.setSpec(dto.getSpec());
        product.setWarranty(dto.getWarranty());
        product.setPurchaseDate(dto.getPurchaseDate());
        product.setTradeLocation(dto.getTradeLocation());
        product.setContact(dto.getContact());
    }

    /**
     * 校验并加载商品，同时确认操作者是发布者本人。
     * <p>
     * 这个检查是"防越权"的关键：前端传的 productId 可以随便改，
     * 如果不校验归属，任何人都能下架、删除别人的商品。
     */
    private Product requireOwner(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_PRODUCT_OWNER);
        }
        return product;
    }

    private void checkCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !Constants.STATUS_ENABLED.equals(category.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "所选分类不存在或已停用");
        }
    }

    private void saveImages(Long productId, List<String> urls) {
        if (CollUtil.isEmpty(urls)) {
            return;
        }
        for (int i = 0; i < urls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(urls.get(i));
            image.setSort(i);
            image.setCreateTime(LocalDateTime.now());
            productImageMapper.insert(image);
        }
    }

    private boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && RoleType.isAdmin(user.getRole());
    }

    /**
     * 显式清空平台处置原因。
     * <p>
     * 必须用 UpdateWrapper：MyBatis-Plus 的 updateById 会忽略实体里为 null 的字段
     * （这个特性平时很有用，可以只更新变化的列），代价就是"想把某个字段改回 null"
     * 时它做不到，只能用它显式 set。这是本项目踩过的坑里最隐蔽的一个。
     */
    private void clearRejectReason(Long productId) {
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getRejectReason, null)
                .eq(Product::getId, productId));
    }
}
