package com.digital.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.constant.Constants;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.User;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.service.AdminDashboardService;
import com.digital.platform.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Override
    public DashboardVO stats() {
        DashboardVO vo = new DashboardVO();

        // 用户统计
        vo.setUserCount(userMapper.selectCount(null));
        vo.setDisabledUserCount(userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getStatus, Constants.STATUS_DISABLED)));

        // 商品统计：每个状态一次 count 查询，共 4 次。
        // 数据量再大也都是走索引的 COUNT，比把所有商品查出来在 Java 里数要快得多。
        vo.setProductCount(productMapper.selectCount(null));
        vo.setPendingCount(countByStatus(ProductStatus.PENDING));
        vo.setOnSaleCount(countByStatus(ProductStatus.ON_SALE));
        vo.setSoldCount(countByStatus(ProductStatus.SOLD));

        // 累计浏览量（手写 SQL 聚合）
        Long totalView = productMapper.sumViewCount();
        vo.setTotalViewCount(totalView == null ? 0L : totalView);

        return vo;
    }

    private Long countByStatus(ProductStatus status) {
        return productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getStatus, status.getCode()));
    }
}
