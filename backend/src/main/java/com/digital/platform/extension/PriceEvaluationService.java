package com.digital.platform.extension;

import com.digital.platform.vo.PriceEvaluationVO;

/**
 * 价格评估（预留扩展点）。
 * <p>
 * 二手交易的痛点之一是「不知道该卖多少」：标高了没人问，标低了亏。
 * 这个接口负责给出「当前标价相对同类商品是偏高还是偏低」的判断。
 * <p>
 * <b>当前实现是基线版</b>：取同分类、同成色、在售商品的成交区间做对比。
 * 它没有考虑品牌溢价、使用时长、是否有拆修这些因素 —— 那是模型该做的事。
 * <p>
 * 换成正式实现时，接口签名不需要改，只是内部从「算术平均」换成
 * 「特征工程 + 回归模型」。前端展示的字段（均价、区间、结论）保持稳定。
 */
public interface PriceEvaluationService {

    /**
     * 评估某个商品当前的标价是否合理。
     *
     * @param productId 商品 ID
     * @return 评估结果；样本不足时返回「数据不足」而不是抛异常 ——
     *         「评不出来」也是一种有效结论，不该让页面报错
     */
    PriceEvaluationVO evaluate(Long productId);
}
