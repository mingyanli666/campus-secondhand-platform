package com.digital.platform.extension;

import com.digital.platform.vo.ProductListVO;

import java.util.List;

/**
 * 智能推荐（预留扩展点）。
 * <p>
 * <b>为什么要有这个接口，而不是直接在 Controller 里写查询？</b>
 * <p>
 * 推荐算法的迭代频率远高于业务代码：今天是"按热度排"，明天可能要换成
 * 协同过滤，后天可能要接一个大模型。如果推荐逻辑散落在 Controller 里，
 * 每次换算法都要动接口层，前端也得跟着回归。
 * <p>
 * 把它收敛成一个接口之后，<b>换算法的成本 = 换一个 @Service 实现类</b>，
 * Controller、API 路径、返回结构全都不用动。
 * <p>
 * <b>当前实现是基线版</b>（按收藏数 + 浏览量排序），不是空实现 ——
 * 空实现既不能演示也不能验收，而且没人知道接口设计得对不对。
 * 基线版先把数据链路跑通，后续换算法时可以直接对比效果。
 *
 * @see com.digital.platform.extension.impl.RecommendServiceImpl 当前基线实现
 */
public interface RecommendService {

    /**
     * 相似商品推荐（商品详情页的「看了又看」）。
     * <p>
     * 基线策略：同分类 + 在售 + 排除自己，按收藏数、浏览量降序。
     *
     * @param productId 当前正在看的商品
     * @param limit     最多返回几条（内部会做范围限制，防前端传超大值）
     */
    List<ProductListVO> similarProducts(Long productId, int limit);

    /**
     * 热门商品（首页推荐位）。
     * <p>
     * 基线策略：在售商品按收藏数、浏览量降序。
     * <p>
     * 注意这里<b>没有做去重和多样性控制</b> —— 结果可能全是同一个卖家的。
     * 这是基线版的已知不足，换成正式算法时一并处理。
     *
     * @param limit 最多返回几条
     */
    List<ProductListVO> hotProducts(int limit);
}
