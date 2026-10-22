package com.digital.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.cache.CategoryCache;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.dto.CategorySaveDTO;
import com.digital.platform.entity.Category;
import com.digital.platform.entity.Product;
import com.digital.platform.mapper.CategoryMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.service.AdminCategoryService;
import com.digital.platform.vo.AdminCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CategoryCache categoryCache;

    @Override
    public List<AdminCategoryVO> listAll() {
        // 管理端要看到全部（含停用的），所以走自定义 SQL 而不是 CategoryService.listEnabled()
        return categoryMapper.selectAdminCategoryList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategorySaveDTO dto) {
        checkNameUnique(dto.getName(), null);

        Category category = new Category();
        applyFields(category, dto);
        categoryMapper.insert(category);
        categoryCache.evict();

        log.info("新增分类 {} - {}", category.getId(), category.getName());
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveDTO dto) {
        requireCategory(id);
        checkNameUnique(dto.getName(), id);

        Category category = new Category();
        category.setId(id);
        applyFields(category, dto);
        categoryMapper.updateById(category);
        categoryCache.evict();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Category category = requireCategory(id);

        // 删除前必须检查有没有商品在用这个分类。
        // 否则删完之后，这些商品的 category_id 就成了悬空引用，
        // 列表里显示"未分类"，卖家也改不回去（下拉框里已经没有这个选项了）。
        Long productCount = productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_HAS_PRODUCT,
                    "该分类下还有 " + productCount + " 件商品，无法删除。可以改为「停用」，"
                            + "停用后发布商品时不再显示该分类，已发布的商品不受影响。");
        }

        // 这里走逻辑删除（Category 有 @TableLogic），数据仍可追溯
        categoryMapper.deleteById(id);
        categoryCache.evict();
        log.info("删除分类 {} - {}", id, category.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireCategory(id);

        Category category = new Category();
        category.setId(id);
        category.setStatus(status);
        categoryMapper.updateById(category);
        categoryCache.evict();
    }

    // ============================================================
    //  私有辅助
    // ============================================================

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    /**
     * 校验分类名唯一。
     * <p>
     * 为什么不在数据库上建唯一索引？因为分类是逻辑删除的：
     * 删掉"平板电脑"之后再建一个同名的，就会被唯一索引挡住 —— 这正是
     * 第一阶段在用户表上避开的那个坑，这里同样避开。
     * 代价是这里的检查存在并发窗口（两个人同时新建同名分类可能都通过），
     * 但对一个后台管理功能来说，这个概率和影响都可以接受。
     *
     * @param excludeId 编辑时排除自己
     */
    private void checkNameUnique(String name, Long excludeId) {
        Long count = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, name)
                .ne(excludeId != null, Category::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ResultCode.CATEGORY_NAME_EXISTS);
        }
    }

    private void applyFields(Category category, CategorySaveDTO dto) {
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
    }
}
