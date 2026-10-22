package com.digital.platform.service;

import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.dto.ProductSaveDTO;
import com.digital.platform.vo.ProductDetailVO;
import com.digital.platform.vo.ProductListVO;

public interface ProductService {

    /** 发布商品，返回新商品ID。新发布的商品状态为「待审核」 */
    Long publish(Long userId, ProductSaveDTO dto);

    /** 编辑商品。编辑后状态回到「待审核」，需要重新过审 */
    void update(Long userId, Long productId, ProductSaveDTO dto);

    /** 删除商品（逻辑删除） */
    void delete(Long userId, Long productId);

    /** 下架商品 */
    void offShelf(Long userId, Long productId);

    /** 重新提交审核（已下架 / 审核驳回 的商品） */
    void resubmit(Long userId, Long productId);

    /** 分页查询商品列表，支持分类、关键词、成色、价格区间、排序 */
    PageResult<ProductListVO> page(ProductQuery query);

    /**
     * 查询商品详情。
     *
     * @param productId     商品ID
     * @param currentUserId 当前登录用户ID，可能为 null（未登录浏览）
     */
    ProductDetailVO detail(Long productId, Long currentUserId);
}
