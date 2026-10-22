package com.digital.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.digital.platform.entity.Category;
import com.digital.platform.vo.AdminCategoryVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 管理后台分类列表（含停用分类，带每个分类下的商品数）。
     * SQL 见 resources/mapper/CategoryMapper.xml
     */
    List<AdminCategoryVO> selectAdminCategoryList();
}
