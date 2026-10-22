package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.digital.platform.entity.Favorite;
import com.digital.platform.vo.ProductListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 我的收藏列表（收藏记录 JOIN 商品）。
     * 收藏表本身只存了 productId，要展示商品信息就必须联表。
     */
    IPage<ProductListVO> selectFavoriteProductPage(IPage<ProductListVO> page, @Param("userId") Long userId);
}
