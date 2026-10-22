package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.digital.platform.common.request.ProductQuery;
import com.digital.platform.entity.Product;
import com.digital.platform.vo.ProductListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 商品列表分页查询（需要关联分类表和用户表）。
     * <p>
     * 为什么不用 BaseMapper 而要手写？因为要 LEFT JOIN 另外两张表拿分类名和卖家昵称。
     * 这类多表查询就是手写 SQL 的用武之地，SQL 写在 resources/mapper/ProductMapper.xml 里。
     * <p>
     * 分页由 MyBatis-Plus 的分页插件自动完成：方法第一个参数是 IPage 时，
     * 它会自动在 SQL 后追加 LIMIT，并额外执行一条 COUNT 查询。
     */
    IPage<ProductListVO> selectProductPage(IPage<ProductListVO> page, @Param("query") ProductQuery query);

    /**
     * 统计所有商品的累计浏览量。
     * <p>
     * 注意手写的 SQL 必须自己写 deleted = 0 —— 逻辑删除只对
     * MyBatis-Plus 自动生成的方法生效，手写的一律不管。
     */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM biz_product WHERE deleted = 0")
    Long sumViewCount();
}
