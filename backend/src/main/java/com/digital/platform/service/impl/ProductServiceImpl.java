package com.digital.platform.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        boolean owner = currentUserId != null && currentUserId.equals(product.getUserId());

        // 未上架的商品只有发布者本人和管理员能看，
        // 否则别人拿到 ID 就能看到还没过审（可能违规）的内容。
        // 注意 isAdmin 会查一次库，所以只在确实需要时才调用，避免每次浏览详情都多一次查询。
        if (!ProductStatus.ON_SALE.getCode().equals(product.getStatus()) && !owner) {
            if (currentUserId == null || !isAdmin(currentUserId)) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
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
        vo.setOwner(owner);
        // 是否已收藏：只有登录用户才有意义，游客一律 false。
        // 这里直接问 FavoriteService，而不是自己再写一遍收藏表的查询 ——
        // "怎么判断收藏"这件事只应该有一处实现。
        vo.setFavorite(currentUserId != null
                && favoriteService.isFavorite(currentUserId, productId));

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

        // 浏览量 +1：用 SQL 原子自增，不能用「查出来 +1 再存回去」，
        // 那样多个用户同时看会互相覆盖，导致计数偏小
        if (!owner) {
            productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                    .setSql("view_count = view_count + 1")
                    .eq(Product::getId, productId));
            vo.setViewCount(product.getViewCount() + 1);
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
