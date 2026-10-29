package com.digital.platform.extension;

import com.digital.platform.vo.CreditRatingVO;

/**
 * 用户信誉评级（预留扩展点）。
 * <p>
 * 二手交易里「这个人靠不靠谱」比「东西好不好」更难判断。
 * 本项目已经有一个 {@code credit_score} 字段（注册时初始 100），
 * 但它只是个静态数字，不会随行为变化。
 * <p>
 * 这个接口负责把「历史行为」折算成信誉等级。
 *
 * <p>
 * <b>当前实现是基线版</b>：只看成交数和被举报次数两个维度。
 * 完整的信誉体系还应该包含：
 * <ul>
 *   <li>买家评价（本项目未做评价功能）</li>
 *   <li>纠纷率、平均响应时长</li>
 *   <li>账号活跃度、注册时长</li>
 *   <li>时间衰减 —— 三年前的差评不该和上个月的一样重</li>
 * </ul>
 *
 * <p>
 * 之所以现在就定义接口，是因为<b>数据从第一天就该开始攒</b>：
 * 订单、举报记录已经在库里了，等真的要做模型时，
 * 发现历史数据没留、要重新开始攒，才是最麻烦的。
 */
public interface CreditRatingService {

    /**
     * 计算某个用户的信誉等级。
     *
     * @param userId 用户 ID
     * @return 等级、各维度明细和一句说明
     */
    CreditRatingVO rating(Long userId);
}
