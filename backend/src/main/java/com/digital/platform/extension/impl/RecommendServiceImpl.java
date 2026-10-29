package com.digital.platform.extension.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.entity.Product;
import com.digital.platform.extension.RecommendService;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能推荐的**基线实现**。
 * <p>
 * <b>它做了什么</b>：按热度（收藏数 → 浏览量）排序，再按分类做粗筛。
 * <p>
 * <b>它没做什么</b>：没有用户画像、没有协同过滤、没有多样性控制。
 * 换句话说，同一件商品推给所有人都是同样的结果 —— 这更像"排行榜"而不是"推荐"。
 * <p>
 * <b>为什么仍然要实现</b>：
 * <ol>
 *   <li>空实现无法验收，也不知道接口设计得对不对</li>
 *   <li>先把「查询 → 组装 → 返回」这条链路跑通，之后换算法只改这一个类</li>
 *   <li>有基线才能对比 —— 换成协同过滤后，得能说清楚"比原来好在哪"</li>
 * </ol>
 *
 * <h3>后续怎么升级（面试可讲）</h3>
 * <pre>
 * 方案一：基于内容的推荐
 *   - 用商品标题/品牌/型号/分类做向量化（TF-IDF 或 embedding）
 *   - 算余弦相似度，取 top N
 *   - 改动范围：只有 similarProducts 的内部实现
 *
 * 方案二：协同过滤
 *   - 数据已经在了：biz_favorite（用户-商品）、biz_order（成交）
 *   - 用户-物品矩阵 → 找相似用户 → 推荐他们看过/买过的
 *   - 冷启动问题：新用户没有行为，回落到当前的热度基线
 *
 * 方案三：接外部模型
 *   - Controller 和 VO 完全不用改，只把本类换成一个调用 HTTP 的实现
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    /** 一次最多返回多少条。上限存在的理由是防止前端传 9999 把整张表拉出来 */
    private static final int MAX_LIMIT = 20;

    private final ProductMapper productMapper;

    @Override
    public List<ProductListVO> similarProducts(Long productId, int limit) {
        Product current = productMapper.selectById(productId);
        if (current == null) {
            // 商品不存在就返回空列表，不抛异常：
            // 推荐位是「锦上添花」的功能，不该让详情页因为它而失败
            return List.of();
        }

        Page<Product> page = new Page<>(1, clampLimit(limit));
        List<Product> records = productMapper.selectPage(page,
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getCategoryId, current.getCategoryId())
                        .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                        // 别把自己推荐给自己
                        .ne(Product::getId, productId)
                        .orderByDesc(Product::getFavoriteCount)
                        .orderByDesc(Product::getViewCount)
                        .orderByDesc(Product::getId)
        ).getRecords();

        return records.stream().map(RecommendServiceImpl::toListVO).toList();
    }

    @Override
    public List<ProductListVO> hotProducts(int limit) {
        Page<Product> page = new Page<>(1, clampLimit(limit));
        List<Product> records = productMapper.selectPage(page,
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                        .orderByDesc(Product::getFavoriteCount)
                        .orderByDesc(Product::getViewCount)
                        .orderByDesc(Product::getId)
        ).getRecords();

        return records.stream().map(RecommendServiceImpl::toListVO).toList();
    }

    /**
     * 限制条数。
     * <p>
     * 注意这是**业务层的第二个上限**：分页插件管的是"每页最多多少条"，
     * 这里管的是"推荐位最多几条"。两回事，别指望前者兜住后者。
     */
    private static int clampLimit(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    /**
     * 实体 → 列表 VO。
     * <p>
     * 刻意不填 categoryName / sellerNickname：那需要额外的 JOIN，
     * 而推荐卡片上只展示标题、价格、封面、成色，多查两次不值得。
     * 需要展示卖家信息时，前端本来就会拿到 sellerId 再做跳转。
     */
    private static ProductListVO toListVO(Product p) {
        ProductListVO vo = new ProductListVO();
        vo.setId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setCoverImage(p.getCoverImage());
        vo.setPrice(p.getPrice());
        vo.setOriginalPrice(p.getOriginalPrice());
        vo.setBrand(p.getBrand());
        vo.setModel(p.getModel());
        vo.setConditionLevel(p.getConditionLevel());
        vo.setWarranty(p.getWarranty());
        vo.setStatus(p.getStatus());
        vo.setViewCount(p.getViewCount());
        vo.setFavoriteCount(p.getFavoriteCount());
        vo.setCreateTime(p.getCreateTime());
        vo.setCategoryId(p.getCategoryId());
        vo.setSellerId(p.getUserId());
        return vo;
    }
}
