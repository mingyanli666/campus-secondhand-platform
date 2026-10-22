package com.digital.platform.service;

import com.digital.platform.common.request.PageQuery;
import com.digital.platform.common.result.PageResult;
import com.digital.platform.vo.ProductListVO;

public interface FavoriteService {

    /** 收藏商品 */
    void add(Long userId, Long productId);

    /** 取消收藏 */
    void remove(Long userId, Long productId);

    /** 是否已收藏，用于商品详情页的按钮状态 */
    boolean isFavorite(Long userId, Long productId);

    /** 我的收藏列表 */
    PageResult<ProductListVO> pageMine(Long userId, PageQuery query);
}
