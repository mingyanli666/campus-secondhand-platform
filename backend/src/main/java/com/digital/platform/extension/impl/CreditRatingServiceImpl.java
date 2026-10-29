package com.digital.platform.extension.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.enums.OrderStatus;
import com.digital.platform.common.enums.ReportStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.Order;
import com.digital.platform.entity.Product;
import com.digital.platform.entity.Report;
import com.digital.platform.entity.User;
import com.digital.platform.extension.CreditRatingService;
import com.digital.platform.mapper.OrderMapper;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.mapper.ReportMapper;
import com.digital.platform.mapper.UserMapper;
import com.digital.platform.vo.CreditRatingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户信誉评级的**基线实现**。
 * <p>
 * 打分公式（简单到可以口头解释，这是刻意的）：
 * <pre>
 *   score = 60                                   起步分
 *         + min(成交数, 10) × 3                  成交是最强的正向信号，最多 +30
 *         + min(发布数, 5)  × 1                  有持续经营行为，最多 +5
 *         - min(被举报成立数, 5) × 10            违规是强负向信号，最多 -50
 *   最终 clamp 到 [0, 100]
 * </pre>
 *
 * <h3>为什么用「成交数」而不是「发布数」做主信号</h3>
 * 发布数谁都能刷（发一堆再删掉），成交数是真金白银的行为 ——
 * 有人愿意付钱，说明这个卖家真的能交付。
 *
 * <h3>这个公式的已知问题（诚实说明）</h3>
 * <ul>
 *   <li><b>没有时间衰减</b>：三年前的一次举报和上个月的一样扣分</li>
 *   <li><b>没有买家评价</b>：本项目没做评价功能，所以"东西好不好"这个最关键的维度缺了</li>
 *   <li><b>阈值是拍的</b>：60 分起步、每单 +3，没有数据支撑，只是先跑起来</li>
 *   <li><b>新用户吃亏</b>：注册就是 60 分（一般），容易让人觉得"这人不靠谱"</li>
 * </ul>
 * 这些恰恰说明了为什么它叫「基线」而不是「实现」——真正的信誉体系需要
 * 大量历史数据和回测才能把权重定下来。
 *
 * <h3>后续怎么升级</h3>
 * <ul>
 *   <li>数据层：新建用户行为表，记录响应时长、纠纷、退款</li>
 *   <li>算法层：逻辑回归 / 梯度提升树，用"是否完成交易"作为标签</li>
 *   <li>展示层：从单一分数变成雷达图（成交、评价、纠纷、活跃）</li>
 * </ul>
 * <b>接口和返回结构不用动</b> —— 各维度本来就已经单独返回了。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditRatingServiceImpl implements CreditRatingService {

    /** 起步分 */
    private static final int BASE = 60;
    /** 成交数上限（超过这个数不再加分，避免老用户永远满分） */
    private static final int SOLD_CAP = 10;
    /** 被举报次数上限（超过这个数不再扣分，防止一次事故把人打到 0） */
    private static final int REPORT_CAP = 5;

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final ReportMapper reportMapper;

    @Override
    public CreditRatingVO rating(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // ① 作为卖家完成的成交数
        long soldCount = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getSellerId, userId)
                .eq(Order::getStatus, OrderStatus.FINISHED.getCode()));

        // ② 发布商品数（逻辑删除的自动排除，MyBatis-Plus 会加条件）
        long productCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getUserId, userId));

        // ③ 被举报且处理成立（HANDLED）的次数
        //    没有"商品 → 卖家"的直接外键，所以先取出该用户的商品 id 集合，
        //    再统计这些商品上的举报。两步查询，对毕业设计的量级完全够用；
        //    真要做大，应该在 report 表上冗余 seller_id，或者走一次 JOIN。
        long reportCount = countHandledReports(userId);

        int score = BASE
                + (int) Math.min(soldCount, SOLD_CAP) * 3
                + (int) Math.min(productCount, 5)
                - (int) Math.min(reportCount, REPORT_CAP) * 10;
        score = Math.max(0, Math.min(100, score));

        CreditRatingVO vo = new CreditRatingVO();
        vo.setUserId(userId);
        vo.setScore(score);
        vo.setLevel(levelOf(score));
        vo.setSoldCount(soldCount);
        vo.setProductCount(productCount);
        vo.setReportCount(reportCount);
        vo.setDescription(describe(score, soldCount, reportCount));
        return vo;
    }

    private long countHandledReports(Long userId) {
        List<Long> productIds = productMapper.selectList(
                        Wrappers.<Product>lambdaQuery()
                                .select(Product::getId)
                                .eq(Product::getUserId, userId))
                .stream().map(Product::getId).toList();

        if (productIds.isEmpty()) {
            return 0;
        }
        return reportMapper.selectCount(Wrappers.<Report>lambdaQuery()
                .in(Report::getProductId, productIds)
                .eq(Report::getStatus, ReportStatus.HANDLED.getCode()));
    }

    private static String levelOf(int score) {
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        return "较差";
    }

    /**
     * 生成一句人话说明。
     * <p>
     * 只有分数没有解释，用户看不出问题出在哪 ——
     * "较差"到底是因为成交少还是因为被举报过？这两种情况的改进方式完全不同。
     */
    private static String describe(int score, long soldCount, long reportCount) {
        if (reportCount > 0) {
            return "有 " + reportCount + " 次被举报且处理成立，信誉受影响";
        }
        if (soldCount >= 5) {
            return "完成 " + soldCount + " 笔交易，履约记录良好";
        }
        if (soldCount > 0) {
            return "已有 " + soldCount + " 笔成交，继续积累可提升等级";
        }
        return "暂无成交记录，完成首笔交易后信誉会提升";
    }
}
