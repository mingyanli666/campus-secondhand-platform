package com.digital.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.entity.Category;
import com.digital.platform.mapper.CategoryMapper;
import com.digital.platform.service.CategoryService;
import com.digital.platform.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listEnabled() {
        List<Category> list = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, Constants.STATUS_ENABLED)
                        .orderByAsc(Category::getSort));
        return list.stream()
                .map(c -> BeanUtil.copyProperties(c, CategoryVO.class))
                .toList();
    }
}
