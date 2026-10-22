package com.digital.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.cache.CategoryCache;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.entity.Category;
import com.digital.platform.mapper.CategoryMapper;
import com.digital.platform.service.CategoryService;
import com.digital.platform.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryCache categoryCache;

    @Override
    public List<CategoryVO> listEnabled() {
        // 先看缓存：分类是纯字典数据，几乎每次商品查询、发布页都要用，
        // 但一个月可能都不会变一次 —— 这是最适合缓存的一类数据
        List<CategoryVO> cached = categoryCache.get();
        if (cached != null) {
            return cached;
        }

        List<Category> list = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, Constants.STATUS_ENABLED)
                        .orderByAsc(Category::getSort));
        List<CategoryVO> vos = list.stream()
                .map(c -> BeanUtil.copyProperties(c, CategoryVO.class))
                .toList();

        categoryCache.put(vos);
        return vos;
    }
}
