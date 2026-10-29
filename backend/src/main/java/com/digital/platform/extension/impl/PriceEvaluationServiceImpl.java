package com.digital.platform.extension.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.digital.platform.common.enums.ProductStatus;
import com.digital.platform.common.exception.BusinessException;
import com.digital.platform.common.result.ResultCode;
import com.digital.platform.entity.Product;
import com.digital.platform.extension.PriceEvaluationService;
import com.digital.platform.mapper.ProductMapper;
import com.digital.platform.vo.PriceEvaluationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 价格评估的**基线实现**。
 * <p>
 * 做法很朴素：取同分类、同成色、在售商品的价格，和当前标价比一比。
 *
 * <h3>为什么设了「样本不足」这条分支</h3>
 * 如果同类商品只有 1 件，算出来的"平均价"就是那一件的价格，
 * 结论毫无意义，还会误导卖家。所以样本少于 3 时直接说"数据不足"。
 * <p>
 * <b>「评不出来」也是一种有效结论</b>，比硬给一个错的好。
 *
 * <h3>为什么阈值是 ±15%</h3>
 * 二手商品本身价格离散度很大（同样的机器，激活过和没激活过能差几百），
 * 阈值设太小会导致几乎所有商品都被判成"偏高/偏低"，反而没有参考价值。
 * ±15% 是让结论"有区分度但不泛滥"的经验值 —— 要换成模型时，
 * 这个阈值会被回归结果取代。
 *
 * <h3>后续怎么升级</h3>
 * <ul>
 *   <li>把品牌溢价、使用时长、是否拆修、是否在保作为特征</li>
 *   <li>用历史成交价（biz_order 里已经有快照价）训练回归模型</li>
 *   <li>输出从「偏高/合理/偏低」变成「预计成交价区间 + 置信度」</li>
 * </ul>
 * 接口签名不变，前端只多展示一两个字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceEvaluationServiceImpl implements PriceEvaluationService {

    /** 少于这个样本数就不下结论 */
    private static final int MIN_SAMPLE = 3;

    /** 偏离均价超过这个比例才算"偏高/偏低" */
    private static final BigDecimal THRESHOLD = new BigDecimal("0.15");

    private final ProductMapper productMapper;

    @Override
    public PriceEvaluationVO evaluate(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // 同分类 + 同成色 + 在售 + 排除自己
        // 成色为空时不做这一维过滤（不是所有品类都填成色）
        List<Product> samples = productMapper.selectList(
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getCategoryId, product.getCategoryId())
                        .eq(StrUtil.isNotBlank(product.getConditionLevel()),
                                Product::getConditionLevel, product.getConditionLevel())
                        .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                        .ne(Product::getId, productId));

        PriceEvaluationVO vo = new PriceEvaluationVO();
        vo.setCurrentPrice(product.getPrice());
        vo.setSampleCount(samples.size());

        if (samples.size() < MIN_SAMPLE) {
            vo.setLevel("数据不足");
            vo.setAdvice("同类在售商品只有 " + samples.size() + " 件，样本太少，暂时无法评估");
            return vo;
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        for (Product sample : samples) {
            BigDecimal price = sample.getPrice();
            if (price == null) {
                continue;
            }
            sum = sum.add(price);
            min = (min == null || price.compareTo(min) < 0) ? price : min;
            max = (max == null || price.compareTo(max) > 0) ? price : max;
        }

        BigDecimal avg = sum.divide(BigDecimal.valueOf(samples.size()), 2, RoundingMode.HALF_UP);
        vo.setAveragePrice(avg);
        vo.setMinPrice(min);
        vo.setMaxPrice(max);

        // 偏离度 = (当前价 - 均价) / 均价
        BigDecimal current = product.getPrice();
        if (current == null || avg.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setLevel("数据不足");
            vo.setAdvice("价格数据不完整，暂时无法评估");
            return vo;
        }
        BigDecimal deviation = current.subtract(avg)
                .divide(avg, 4, RoundingMode.HALF_UP);

        if (deviation.compareTo(THRESHOLD.negate()) < 0) {
            vo.setLevel("偏低");
            vo.setAdvice("比同类均价低 " + percent(deviation.abs()) + "，成交会更快，但注意别亏太多");
        } else if (deviation.compareTo(THRESHOLD) > 0) {
            vo.setLevel("偏高");
            vo.setAdvice("比同类均价高 " + percent(deviation) + "，可能不容易卖出去，建议参考区间下调");
        } else {
            vo.setLevel("合理");
            vo.setAdvice("与同类均价基本持平（参考 " + samples.size() + " 件同分类同成色商品）");
        }
        return vo;
    }

    private static String percent(BigDecimal ratio) {
        return ratio.multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }
}
