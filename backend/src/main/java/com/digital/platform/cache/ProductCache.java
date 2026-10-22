package com.digital.platform.cache;

import com.digital.platform.common.constant.CacheKeys;
import com.digital.platform.vo.ProductDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品详情缓存。
 * <p>
 * 这里集中处理了缓存的三个经典问题，也是面试必问的三连：
 * <ol>
 *   <li><b>缓存穿透</b>：查询一个不存在的商品 ID 时，请求会绕过缓存直接打到数据库。
 *       恶意构造不存在的 ID 循环请求，数据库就被打穿了。
 *       解法是<b>把"不存在"也缓存起来</b>（空值哨兵），但 TTL 要短（本项目 2 分钟），
 *       否则商品后来真的创建了，也要等很久才能查到。</li>
 *
 *   <li><b>缓存雪崩</b>：大量 key 在同一时刻过期，请求瞬间全打到数据库。
 *       解法是给 TTL 加<b>随机抖动</b>，让过期时间分散开。</li>
 *
 *   <li><b>降级</b>：Redis 挂掉时不能把整个业务带崩。
 *       这里的每个方法都吞掉缓存异常、打日志、返回"未命中"，
 *       请求自然回落到数据库 —— 性能下降，但功能可用。</li>
 * </ol>
 * <p>
 * 注意：<b>不缓存"当前用户是否收藏"</b>。那个字段是用户相关的，
 * 缓存进去会让 A 用户看到 B 用户的收藏状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCache {

    /** 基础 TTL */
    private static final Duration DETAIL_TTL = Duration.ofMinutes(30);

    /** 抖动上限：在基础 TTL 上再随机加 0~5 分钟，打散过期时间 */
    private static final long JITTER_MAX_SECONDS = Duration.ofMinutes(5).getSeconds();

    /** 空值哨兵的存活时间，要明显短于正常数据 */
    private static final Duration EMPTY_TTL = Duration.ofMinutes(2);

    /** 用一个特殊字符串表示"这个 ID 在数据库里确实不存在" */
    private static final String EMPTY_SENTINEL = "__EMPTY__";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * 读取缓存的商品详情。
     *
     * @return 缓存未命中或缓存异常时返回 null；不会返回"空值哨兵"
     */
    public ProductDetailVO getDetail(Long productId) {
        try {
            String json = redis.opsForValue().get(CacheKeys.productDetail(productId));
            if (json == null || EMPTY_SENTINEL.equals(json)) {
                return null;
            }
            // 每次反序列化都会得到新对象，所以调用方随便改都不会污染缓存
            return objectMapper.readValue(json, ProductDetailVO.class);
        } catch (Exception e) {
            // 缓存层出任何问题都不能影响业务，降级为查库
            log.warn("读取商品详情缓存失败，降级查库 id={}: {}", productId, e.getMessage());
            return null;
        }
    }

    /** 是否已经确认过"这个商品不存在"（空值缓存命中） */
    public boolean isKnownMissing(Long productId) {
        try {
            return EMPTY_SENTINEL.equals(redis.opsForValue().get(CacheKeys.productDetail(productId)));
        } catch (Exception e) {
            return false;
        }
    }

    public void putDetail(Long productId, ProductDetailVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            redis.opsForValue().set(CacheKeys.productDetail(productId), json, ttlWithJitter());
        } catch (Exception e) {
            log.warn("写入商品详情缓存失败 id={}: {}", productId, e.getMessage());
        }
    }

    public void putEmpty(Long productId) {
        try {
            redis.opsForValue().set(CacheKeys.productDetail(productId), EMPTY_SENTINEL, EMPTY_TTL);
        } catch (Exception e) {
            log.warn("写入空值缓存失败 id={}: {}", productId, e.getMessage());
        }
    }

    /** 删除缓存。所有会改变商品可见内容的写操作都必须调用它 */
    public void evict(Long productId) {
        try {
            redis.delete(CacheKeys.productDetail(productId));
        } catch (Exception e) {
            log.warn("删除商品详情缓存失败 id={}: {}", productId, e.getMessage());
        }
    }

    private Duration ttlWithJitter() {
        return DETAIL_TTL.plusSeconds(ThreadLocalRandom.current().nextLong(JITTER_MAX_SECONDS));
    }
}
