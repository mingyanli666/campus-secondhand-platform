package com.digital.platform.service;

import com.digital.platform.dto.CategorySaveDTO;
import com.digital.platform.vo.AdminCategoryVO;

import java.util.List;

public interface AdminCategoryService {

    /** 全部分类（含停用），带商品数统计 */
    List<AdminCategoryVO> listAll();

    /** 新增分类，返回新分类 ID */
    Long create(CategorySaveDTO dto);

    /** 编辑分类 */
    void update(Long id, CategorySaveDTO dto);

    /** 删除分类（有商品时不允许删除） */
    void delete(Long id);

    /** 启用 / 停用分类 */
    void updateStatus(Long id, Integer status);
}
