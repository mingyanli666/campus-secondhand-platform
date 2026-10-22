package com.digital.platform.service;

import com.digital.platform.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    /** 查询所有启用的分类，按 sort 升序 */
    List<CategoryVO> listEnabled();
}
