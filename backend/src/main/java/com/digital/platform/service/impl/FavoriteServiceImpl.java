package com.digital.platform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.request.PageQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.Favorite;
import com.digital.platform.entity.Product;
import com.digital.platform.mapper.FavoriteMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.FavoriteService;
import com.digital.platform.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long productId) {
        Product product = requireProduct(productId);

        // 不能收藏自己的商品 —— 否则卖家可以自己给自己刷收藏数，制造"很抢手"的假象
        if (product.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FAVORITE_SELF);
        }
        // 只在售的商品才能收藏。已下架/已售出/待审核的收藏了也没意义
        if (!ProductStatus.ON_SALE.getCode().equals(product.getStatus())) {
            // 带上具体状态，而不是笼统的"已下架或已售出" ——
            // 用户看到的可能是"待审核"的商品，提示要能对上他眼前的画面
            throw new BusinessException(ResultCode.PRODUCT_NOT_ON_SALE,
                    "该商品当前状态为「" + ProductStatus.descOf(product.getStatus()) + "」，暂时无法收藏");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreateTime(LocalDateTime.now());

        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 靠 (user_id, product_id) 唯一索引兜并发。
            // 比"先查有没有再插入"可靠：两个请求同时点收藏，先查后插会双双通过检查。
            throw new BusinessException(ResultCode.ALREADY_FAVORITE);
        }

        // 用 SQL 原子自增，理由和浏览量一样：先查再改会丢计数
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .setSql("favorite_count = favorite_count + 1")
                .eq(Product::getId, productId));

        log.debug("用户 {} 收藏商品 {}", userId, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long productId) {
        // 带 userId 条件删除：只能取消自己的收藏
        int rows = favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (rows == 0) {
            throw new BusinessException(ResultCode.NOT_FAVORITE);
        }

        // GREATEST(..., 0) 保证计数不会被减成负数。
        // 虽然正常流程下不会发生，但数据一旦出现负数，页面上显示的"收藏 -1"会非常刺眼，
        // 而这种脏数据事后很难定位。加个下限的成本几乎为零。
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .setSql("favorite_count = GREATEST(favorite_count - 1, 0)")
                .eq(Product::getId, productId));

        log.debug("用户 {} 取消收藏商品 {}", userId, productId);
    }

    @Override
    public boolean isFavorite(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0;
    }

    @Override
    public PageResult<ProductListVO> pageMine(Long userId, PageQuery query) {
        Page<ProductListVO> page = query.toPage();
        IPage<ProductListVO> result = favoriteMapper.selectFavoriteProductPage(page, userId);
        return PageResult.of(result);
    }

    private Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }
}
