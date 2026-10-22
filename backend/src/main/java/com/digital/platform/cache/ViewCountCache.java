package com.digital.platform.cache;

import com.digital.platform.common.constant.CacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 商品浏览量计数器。
 * <p>
 * <b>为什么要把浏览量的"写"挪到 Redis？</b>
 * <p>
 * 原来的实现是每次看详情都执行 {@code UPDATE biz_product SET view_count = view_count + 1}。
 * 这在热门商品上是典型的<b>热点行竞争</b>：
 * 同一行的行锁把这些更新串行化了，并发越高、这个 UPDATE 越慢，
 * 而它恰恰处在商品详情这条最热的读路径上。
 * <p>
 * 改成 Redis 自增之后：
 * <ul>
 *   <li>内存自增，无锁竞争，微秒级</li>
 *   <li>由定时任务每 5 分钟把增量<b>批量</b>加回数据库，
 *       一个商品的 1000 次浏览只产生 1 条 UPDATE</li>
 * </ul>
 * <p>
 * <b>代价</b>：Redis 如果在这一刻挂掉，最多丢失 5 分钟的浏览量。
 * 浏览量只是统计数字、不影响交易，所以这个代价可以接受 ——
 * 但如果是订单金额，就绝对不能这么做。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountCache {

    private final StringRedisTemplate redis;

    /**
     * 浏览量 +1，并返回自上次回写以来累积的增量。
     *
     * @return 当前增量；Redis 不可用时返回 -1，调用方据此降级为直接改库
     */
    public long increment(Long productId) {
        try {
            Long delta = redis.opsForValue().increment(CacheKeys.productViewDelta(productId));
            // 同时登记到待回写集合，定时任务按集合遍历，避免 SCAN 全库
            redis.opsForSet().add(CacheKeys.productViewPending(), String.valueOf(productId));
            return delta == null ? -1L : delta;
        } catch (Exception e) {
            log.warn("浏览量计数失败，降级为直接更新数据库 id={}: {}", productId, e.getMessage());
            return -1L;
        }
    }

    /** 读取当前累积的增量，不存在时返回 0（用于拼接展示值） */
    public long getDelta(Long productId) {
        try {
            String v = redis.opsForValue().get(CacheKeys.productViewDelta(productId));
            return v == null ? 0L : Long.parseLong(v);
        } catch (Exception e) {
            return 0L;
        }
    }
}
